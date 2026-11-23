# OctoClient Cosmetics API

Damit man die Kosmetics **anderer OctoClient-Nutzer auf jedem Server** sieht
(auch auf pvphub, Hypixel usw.), ohne dass der Server die Mod installiert hat.

```
Spieler A ──POST /v1/loadouts──►  dieser Dienst  ◄──GET /v1/loadouts── Spieler B
```

Jeder Client lädt sein eigenes Loadout unter seiner UUID hoch und fragt alle
5 Sekunden die Loadouts der Spieler in seiner Umgebung ab.

## Starten

```bat
python octoclient_api.py
```

Standard: `0.0.0.0:8765`, Datenbank `loadouts.db` daneben, kein Token.

Sinnvolle Varianten:

```bat
python octoclient_api.py --local                 :: nur 127.0.0.1 (nur eigener PC / LAN-Test)
python octoclient_api.py --port 9000 --token geheim
python octoclient_api.py --prune-days 7          :: alte Einträge nach 7 Tagen löschen
```

Nur Python 3.8+ nötig, keine Abhängigkeiten (`pip install` nicht erforderlich).

## Client konfigurieren

Datei `run/config/octoclient_api.json` (wird beim ersten Start automatisch
angelegt):

```json
{
  "enabled": true,
  "url": "http://127.0.0.1:8765",
  "token": "",
  "pollSeconds": 5
}
```

- `url` → Adresse des Dienstes. Für Freunde im Internet: die öffentliche
  Adresse deines Servers, z. B. `https://cosmetics.deinedomain.de`
  (am besten hinter nginx/Caddy mit HTTPS).
- `token` → muss dem `--token` des Servers entsprechen, sonst leer lassen.
- `enabled` → `false` schaltet die geteilten Kosmetics komplett ab.

Nach Änderungen den Client neu starten (die Datei wird beim ersten Tick
gelesen).

## Endpunkte

| Methode | Pfad | Zweck |
|---|---|---|
| `GET` | `/health` | Status + Anzahl Einträge |
| `POST` | `/v1/loadouts` | Loadout speichern (`{"uuid","name","loadout"}`) |
| `GET` | `/v1/loadouts?uuid=..&uuid=..` | bis zu 100 Loadouts abfragen |
| `DELETE` | `/v1/loadouts/<uuid>` | Eintrag löschen |

Das `loadout`-Feld beginnt mit dem 9-Feld-String
(`bandana|wings|hat|hatColor|halo|haloColor|aura|auraColor|auraShape`).
Optional folgen Cape, Accessory, Hat-Design, MiniMe-Skin-Benutzername,
Katalogauswahl und Nametag-Einstellungen. Ein späteres Feld benötigt leere Platzhalter für ausgelassene
frühere Felder, z. B.
`Black Lightning Bandana|Black Onyx Wings|0|0|0|0|0|0|Ring|||Fedora`.
Ältere Loadouts bleiben lesbar.

## Hosting-Hinweise

- Der Dienst ist absichtlich dumm: er speichert nur UUID → Loadout-String.
- Für öffentliches Hosting `--token` setzen (Clients senden dann
  `Authorization: Bearer <token>`), sonst kann jeder Einträge überschreiben.
- HTTPS-Terminierung macht ein Reverse-Proxy (Caddy: 3 Zeilen, nginx analog).
- Rate-Limit ist eingebaut (20 Requests/s pro IP, Burst 60).
- `loadouts.db` sichern, wenn dir die Einträge wichtig sind; ansonsten sind sie
  jederzeit neu hochladbar.
