# Orientation - Frontoffice + Backoffice

## What was added

A complete backoffice module was added with JavaFX + JDBC:

- Question CRUD
- Reponse CRUD
- Dashboard and navigation between backoffice screens
- Auto schema creation for `questions` and `reponses` tables

## Entry points

- Frontoffice: `MainApp`
- Backoffice: `MainBackoffice`

## Run

```powershell
Set-Location "C:\pi\asma_pi\Orientation"
mvn -DskipTests compile
```

Run frontoffice:

```powershell
Set-Location "C:\pi\asma_pi\Orientation"
mvn javafx:run
```

Run backoffice:

```powershell
Set-Location "C:\pi\asma_pi\Orientation"
mvn -Pbackoffice javafx:run
```

## DB note

Connection is configured in `src/main/java/utils/DBConnection.java`:

- URL: `jdbc:mysql://localhost:3306/orientation`
- USER: `root`
- PASSWORD: empty

If DB is unavailable, the app starts but CRUD actions will show errors.

