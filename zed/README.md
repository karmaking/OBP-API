# Zed IDE Configuration for OBP-API

This folder contains the recommended Zed IDE configuration for the OBP-API project. Each developer can set up their own personalized Zed environment while maintaining consistent project settings.

## Quick Setup

Run the setup script to copy the configuration files to your local `.zed` folder:

**Linux/macOS:**
```bash
./zed/setup-zed.sh
```

**Windows:**
```cmd
zed\setup-zed.bat
```

This will create a `.zed` folder in the project root with the recommended settings.

## What's Included

### 📁 Configuration Files

- **`settings.json`** - IDE settings optimized for Scala/OBP-API development
- **`tasks.json`** - Predefined tasks for building, running, and testing the project

### ⚙️ Key Settings

- **Format on save: DISABLED** - Preserves your code formatting choices
- **Scala LSP (Metals) configuration** - Optimized for Maven-based Scala projects
- **UI preferences** - Consistent theme, font sizes, and panel layout
- **Semantic highlighting** - Enhanced code readability

## Available Tasks

Access tasks in Zed with `Cmd/Ctrl + Shift + P` → "task: spawn"

### 🚀 Development Tasks

| Task | Command | Description |
|------|---------|-------------|
| **[1] Run OBP-API Server** | `mvn jetty:run -pl obp-api` | Starts the OBP-API server on port 8080 |
| **[2] Test API Root Endpoint** | `curl http://localhost:8080/obp/v5.1.0/root` | Quick API health check |
| **[3] Compile Only** | `mvn compile -pl obp-api` | Compiles the project without running tests |

### 🔨 Build Tasks

| Task | Command | Description |
|------|---------|-------------|
| **[4] Build OBP-API** | `mvn install -pl .,obp-commons -am -DskipTests` | Full build excluding tests |
| **[5] Clean Target Folders** | `mvn clean` | Removes all compiled artifacts |

### 🧪 Testing & Validation

| Task | Command | Description |
|------|---------|-------------|
| **[7] Run Tests** | `mvn test -pl obp-api` | Executes the project test suite |
| **[8] Maven Validate** | `mvn validate` | Validates project structure and dependencies |
| **[9] Check Dependencies** | `mvn dependency:resolve` | Downloads and verifies all dependencies |

### 🛠️ Utility Tasks

| Task | Command | Description |
|------|---------|-------------|
| **[6] Kill OBP-API Server** | `lsof -ti:8080 \| xargs kill -9` | Terminates any process running on port 8080 |

## Typical Development Workflow

1. **Initial Setup**: `[4] Build OBP-API` - Build the project
2. **Development**: `[3] Compile Only` - Quick compilation during development
3. **Testing**: `[1] Run OBP-API Server` → `[2] Test API Root Endpoint`
4. **Cleanup**: `[6] Kill OBP-API Server` when done

## Maven Configuration

All Maven tasks use optimized JVM settings:
```
MAVEN_OPTS="-Xss128m --add-opens=java.base/java.util.jar=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"
```

These settings resolve Java module system compatibility issues with newer JDK versions.

## Customization

The `.zed` folder is in `.gitignore`, so you can:

- Modify settings without affecting other developers
- Add personal tasks or shortcuts
- Adjust themes, fonts, and UI preferences
- Configure additional language servers

### Example Customizations

**Add a custom task** (in `.zed/tasks.json`):
```json
{
  "label": "My Custom Task",
  "command": "echo",
  "args": ["Hello World"],
  "use_new_terminal": false
}
```

**Change theme** (in `.zed/settings.json`):
```json
{
  "theme": "Ayu Dark"
}
```

## Troubleshooting

### Port 8080 Already in Use
```bash
# Use task [6] or run manually:
lsof -ti:8080 | xargs kill -9
```

### Metals LSP Issues
1. Restart Zed IDE
2. Run `[8] Maven Validate` to ensure project structure is correct
3. Check that Java 11+ is installed and configured

### Build Failures
1. Run `[5] Clean Target Folders`
2. Run `[9] Check Dependencies` 
3. Run `[4] Build OBP-API`

## Support

For Zed IDE-specific issues, consult the [Zed documentation](https://zed.dev/docs).
For OBP-API project issues, refer to the main project README.