=====================================================
  AgentOps — Required Libraries
=====================================================

This folder holds the external JAR files the project needs.

REQUIRED: SQLite JDBC Driver
─────────────────────────────
1. Go to: https://github.com/xerial/sqlite-jdbc/releases
2. Download: sqlite-jdbc-3.xx.x.jar   (pick the latest version)
3. Put the JAR file in this /lib folder
4. In IntelliJ IDEA:
   File → Project Structure → Modules → Dependencies tab
   Click the "+" button → JARs or directories
   Navigate to this /lib folder and select the JAR
   Click OK → Apply

OPTIONAL: Switch to MySQL instead of SQLite
─────────────────────────────────────────────
If you want to use MySQL (requires MySQL Server running):
1. Download MySQL Connector/J from: https://dev.mysql.com/downloads/connector/j/
2. Add it the same way as above
3. Open src/db/DatabaseHelper.java
4. Uncomment the MySQL connection line and comment out the SQLite line
5. Update the DB_URL, DB_USER, DB_PASSWORD constants

NOTE: SQLite is recommended for demos because it needs zero setup.
The database file (agentops.db) is created automatically on first run.
=====================================================
