# PlayerBounties

![Paper](https://img.shields.io/badge/Paper-1.21.11-22c55e?style=for-the-badge)
![Java](https://img.shields.io/badge/Java-21-f97316?style=for-the-badge&logo=openjdk)
![Version](https://img.shields.io/badge/version-1.0.1-111827?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-2563eb?style=for-the-badge)

Игроки назначают награду в алмазах за убийство другого игрока.

## Версия

PlayerBounties 1.0.1

Paper 1.21.11  
API 1.21.11-R0.1-SNAPSHOT  
Java 21

## Команды

`/bounty set <player> <diamonds>` - назначить награду
`/bounty list` - список наград
`/bounty reload` - перезагрузить конфиг

## Permission

`playerbounties.use`
`playerbounties.reload`
Reload по умолчанию доступен op.

## Функции

- цена хранится в алмазах;
- награды складываются;
- при убийстве награда уходит киллеру;
- данные сохраняются в конфиг.

## Сборка

```bash
./gradlew build
```

Готовый `.jar` будет в `build/libs/`.
