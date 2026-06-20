# HTTP Block

A Fabric mod that lets redstone systems talk to HTTP services.

Two blocks:

- **HTTP Sender** — sends an HTTP `POST` request on a redstone rising edge.
- **HTTP Receiver** — polls a URL and emits a short redstone pulse (like an Observer) when a *new* event arrives.

It feels like redstone. The receiver detects **events**, not state — it never outputs a permanent signal strength.

```
Farm Full → Comparator → HTTP Sender → FastAPI
Discord Bot → Relay API → HTTP Receiver → Redstone Pulse
```

## Requirements

| Component | Version |
|-----------|---------|
| Minecraft | 26.1.2 (new year-based versioning) |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.152.1+ |
| Java | 25+ |

> Minecraft 26.1.2 requires **Java 25**. The Gradle build auto-provisions a
> Java 25 toolchain via the Foojay resolver, so you only need Java 21+ installed
> on your machine to run Gradle itself.

## Building

```bash
./gradlew build
```

The final remapped mod jar is emitted to `build/libs/http-block-1.0.0.jar`.
A ready-to-use copy is placed at the project root as **`HTTPBlock.jar`**.

Drop `HTTPBlock.jar` (plus Fabric API) into your server/client `mods` folder.

## HTTP Sender

Right-click the block to open its GUI:

- **URL** — destination endpoint, e.g. `http://localhost:8000/farm-full`
- **Payload** — raw JSON body, e.g. `{"message":"Farm Full"}`

Behaviour:

- Triggered **only on a rising edge** (OFF → ON). One request per power transition.
- No repeated requests while it stays powered.
- Method is always `POST`.
- `Content-Type: application/json`.
- Requests are fully asynchronous and never block the server thread.

```
Redstone Power → Sender Block → HTTP POST → Target Server
```

## HTTP Receiver

Right-click the block to open its GUI:

- **URL** — polling endpoint, e.g. `http://localhost:8000/event`
- **Last Message** — read-only, shows the message from the most recent new event.

Behaviour:

- A single global **Poll Manager** ticks every receiver. No per-block timers.
- Default poll rate: **20 ticks (1s)**. Change with `/http pollrate <ticks>`.
- On each poll the receiver fetches a JSON event (see format below).
- It compares the event's `hash` to the last seen hash.
  - **Different hash** → store hash + message, emit a redstone pulse.
  - **Same hash** → do nothing (no duplicate pulses).
- Pulse strength is fixed at **15**.
- Pulse duration defaults to **20 ticks (1s)**. Change with `/http pulselength <ticks>`.
- Acts like an Observer: power 15 for the duration, then back to 0.
- Plays a redstone-tick click sound on each pulse.

```
New Event → Power 15 → 20 Ticks → Power 0
```

## Event Format

The receiver's polling endpoint must return JSON with three fields:

```json
{
  "timestamp": 1750412345,
  "message": "HI",
  "hash": "6a8f6c5d..."
}
```

| Field | Type | Description |
|-------|------|-------------|
| `timestamp` | int | Unix timestamp when the event was created |
| `message` | string | The message (displayed in the receiver GUI) |
| `hash` | string | Unique event identifier; the receiver only pulses when this changes |

The `hash` is what the receiver uses to detect *new* events. A common way to
generate it is `SHA256(timestamp + ":" + message)`, but any unique-per-event
string works. The receiver does not interpret the message — only the hash matters.

## Commands

```
/http pollrate <ticks>      Set the global poll rate (1–1200 ticks)
/http pulselength <ticks>   Set the receiver pulse length (1–1200 ticks)
/http status                Show current config + sender/receiver counts
/http reload                Reload config from disk
```

Config is stored in `config/http-block.properties`:

```properties
pollRate=20
pulseLength=20
```

## Building the Relay / Receiver Client

The receiver needs a small HTTP service that:

1. Accepts `POST /event` to record a new event.
2. Serves `GET /event` returning the latest event as JSON with a `hash`.

Any language/framework works. Below is a complete **FastAPI** example.

### FastAPI Relay (Python)

```python
from fastapi import FastAPI
from pydantic import BaseModel
import hashlib
import time

app = FastAPI()

latest_event = {
    "timestamp": 0,
    "message": "",
    "hash": "",
}


class EventIn(BaseModel):
    message: str


@app.post("/event")
async def create_event(data: EventIn):
    global latest_event
    message = str(data.message)
    timestamp = int(time.time())
    event_hash = hashlib.sha256(f"{timestamp}:{message}".encode()).hexdigest()
    latest_event = {
        "timestamp": timestamp,
        "message": message,
        "hash": event_hash,
    }
    return {"success": True}


@app.get("/event")
async def get_event():
    return latest_event
```

Run it:

```bash
pip install fastapi uvicorn
uvicorn relay:app --port 8000
```

### Wiring it up

1. Start the relay above on `http://localhost:8000`.
2. In Minecraft, place an **HTTP Sender**, open it, set:
   - URL: `http://localhost:8000/event`
   - Payload: `{"message":"Farm Full"}`
3. Place an **HTTP Receiver**, open it, set:
   - URL: `http://localhost:8000/event`
4. Power the sender with a comparator / lever / button.
5. The relay stores a new event (with a new hash).
6. The receiver polls, sees the new hash, and emits one redstone pulse.

### Sender payload

The sender sends whatever you typed in the Payload field as the raw request body
with `Content-Type: application/json`. You are responsible for making it valid
JSON that your server expects. Example payloads:

```json
{"message":"Farm Full"}
```

```json
{"event":"door_open","player":"Steve"}
```

```json
{"text":"Farm is full","color":"red"}
```

## Example Use Cases

- **Farm notification** — comparator on a full chest → sender → relay → receiver
  somewhere else in the world pulses, ringing a note block.
- **Discord-controlled door** — Discord bot POSTs to the relay; the Minecraft
  receiver pulses and opens a door.
- **Home Assistant** — a real-world motion sensor triggers Home Assistant, which
  POSTs to the relay; the receiver pulses in-game.

## Technical Notes

- All HTTP work happens on a dedicated daemon thread pool via
  `java.net.http.HttpClient` + `CompletableFuture`. The main server thread is
  never blocked.
- Each receiver keeps an `AtomicBoolean` so overlapping polls for the same block
  are coalesced.
- Block data (URL, payload, last hash, last message, pulse ticks) is persisted in
  the block entity and survives reloads.
- The blocks have a `powered` blockstate property with dedicated powered textures
  on all sides, so the block visibly lights up when active.

## License

CC0-1.0.
