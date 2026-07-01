# SlowMining

Wtyczka do Spigot/Paper (Minecraft 1.20.x), która sprawia, że wybrane bloki
kopie się dłużej — nakłada efekt Mining Fatigue tylko wtedy, gdy gracz
faktycznie kopie skonfigurowany blok.

## Jak to działa

- Gdy gracz zaczyna kopać blok z listy w `config.yml`, dostaje efekt
  Mining Fatigue o poziomie ustawionym dla tego bloku.
- Efekt jest odświeżany co kilka ticków, dopóki gracz nadal patrzy/kopie
  ten sam blok.
- Gdy blok zostanie skopany, gracz przestanie patrzeć na blok, zmieni
  cel albo wyloguje się — efekt jest usuwany.

## Budowanie (wymaga internetu i Mavena)

```bash
mvn clean package
```

Gotowy plik `.jar` pojawi się w katalogu `target/slowmining.jar`.

Jeśli nie masz Mavena zainstalowanego lokalnie, otwórz ten folder
w IntelliJ IDEA / Eclipse jako projekt Maven i użyj przycisku
"Build" / "Package", albo zainstaluj Maven z https://maven.apache.org/.

## Instalacja na serwerze

1. Zbuduj plik `slowmining.jar` (patrz wyżej).
2. Wrzuć go do folderu `plugins/` na serwerze Spigot lub Paper.
3. Uruchom/zrestartuj serwer — utworzy się plik `plugins/SlowMining/config.yml`.
4. Edytuj `config.yml`, żeby dodać/zmienić bloki i poziomy spowolnienia.
5. Wpisz `/slowmining reload`, żeby przeładować konfigurację bez restartu.

## Konfiguracja (config.yml)

```yaml
blocks:
  DIAMOND_ORE:
    amplifier: 2
  OBSIDIAN:
    amplifier: 3
  ANCIENT_DEBRIS:
    amplifier: 2

ignore-if-creative: true
```

- Klucz to nazwa materiału Minecraft (wielkimi literami), np. `STONE`,
  `IRON_ORE`, `OBSIDIAN`. Pełna lista:
  https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html
- `amplifier` to poziom Mining Fatigue:
  - `0` – lekkie spowolnienie
  - `1`–`2` – wyraźnie wolniej
  - `3`+ – bardzo wolno, praktycznie nie da się skopać gołymi rękami
- `ignore-if-creative` – jeśli `true`, efekt nie działa w trybie kreatywnym.

## Uprawnienia

- `slowmining.admin` (domyślnie: op) – pozwala użyć `/slowmining reload`.
