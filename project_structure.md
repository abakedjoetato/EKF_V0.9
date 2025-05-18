# Deadside Discord Bot - Project Structure

This document outlines the structure of the Deadside Discord bot project, detailing its organization, components, and relationships between different modules.

## Package Organization

The project follows standard Java package conventions:

- `com.deadside.bot` - Root package
  - `.bot` - Core bot functionality
  - `.commands` - Command implementations
    - `.admin` - Admin/owner commands
    - `.economy` - Economy and currency commands 
    - `.faction` - Faction management commands
    - `.player` - Player interaction commands
    - `.stats` - Statistics and leaderboard commands
  - `.config` - Configuration management
  - `.db` - Database interaction
    - `.models` - Data models
    - `.repositories` - Data access objects
  - `.listeners` - Event listeners
  - `.parsers` - Game log and CSV parsers
  - `.premium` - Premium feature management
  - `.schedulers` - Scheduled tasks
  - `.sftp` - SFTP connections to game servers
  - `.utils` - Utility classes

## Core Components

### 1. Bot Initialization
- `Main.java` - Entry point
- `DeadsideBot.java` - Bot initialization and lifecycle

### 2. Configuration
- `Config.java` - Singleton configuration manager
  - Loads settings from config.properties and environment variables
  - Handles hard-coded security elements like owner ID

### 3. Command System
- `ICommand.java` - Interface for all commands
- `CommandManager.java` - Registers and manages commands
- Command implementations are categorized by function

### 4. Database Integration
- `MongoDBConnection.java` - MongoDB Atlas connection manager
- Repository classes for each data model
- Models for players, servers, factions, etc.

### 5. Event Handling
- `CommandListener.java` - Handles slash commands
- `ButtonListener.java` - Handles button interactions
- `ModalListener.java` - Handles modal form submissions
- `StringSelectMenuListener.java` - Handles dropdown menus

### 6. Game Server Integration
- `SftpManager.java` - Manages connections to game servers
- Log parsers for extracting game events
- CSV parsers for killfeed data

### 7. Scheduled Tasks
- `KillfeedScheduler.java` - Processes killfeed data every 5 minutes

## Important Design Patterns

1. **Command Pattern** - Commands implement the `ICommand` interface
2. **Repository Pattern** - Data access through repository classes
3. **Singleton Pattern** - For config, database, and manager classes
4. **Observer Pattern** - For event listeners
5. **Strategy Pattern** - For parsers with different implementations

## Key Interaction Flows

### Command Execution Flow
1. User triggers slash command in Discord
2. `CommandListener` receives event
3. `CommandManager` routes to appropriate command implementation
4. Command calls `deferReply()` immediately
5. Command performs business logic
6. Command sends response via `event.getHook().sendMessage()`

### Game Data Processing Flow
1. `KillfeedScheduler` triggers at scheduled intervals
2. SFTP connection retrieves log files from game server
3. Parser processes the logs and extracts data
4. Data is stored in MongoDB
5. Notifications sent to Discord channels as needed

## File Organization

- `src/main/java/` - Java source files
- `src/main/resources/` - Configuration files
- `pom.xml` - Maven project configuration

## Dependencies

- **JDA** - Java Discord API
- **MongoDB Driver** - For database connectivity
- **JSch** - For SFTP connections
- **SLF4J/Logback** - For logging
- **OpenCSV** - For CSV parsing

This structure ensures modularity, maintainability, and separation of concerns throughout the project.