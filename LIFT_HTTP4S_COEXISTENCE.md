# Lift and http4s Coexistence Strategy

## Question
Can http4s and Lift coexist in the same project to convert endpoints one by one?

## Answer: Yes, on Different Ports

### Architecture Overview

```
┌─────────────────────────────────────────┐
│         OBP-API Application             │
├─────────────────────────────────────────┤
│                                         │
│  ┌──────────────┐  ┌─────────────────┐ │
│  │ Lift/Jetty   │  │ http4s Server   │ │
│  │ Port 8080    │  │ Port 8081       │ │
│  └──────────────┘  └─────────────────┘ │
│         │                  │            │
│         └──────┬───────────┘            │
│                │                        │
│         Shared Resources:               │
│         - Database                      │
│         - Business Logic                │
│         - Authentication                │
│         - ResourceDocs                  │
└─────────────────────────────────────────┘
```

### How It Works

1. **Two HTTP Servers Running Simultaneously**
   - Lift/Jetty continues on port 8080
   - http4s starts on port 8081
   - Both run in the same JVM process

2. **Shared Components**
   - Database connections
   - Business logic layer
   - Authentication/authorization
   - Configuration
   - Connector layer

3. **Gradual Migration**
   - Start: All endpoints on Lift (port 8080)
   - During: Some on Lift, some on http4s
   - End: All on http4s (port 8081), Lift removed

## Implementation Approach

### Step 1: Add http4s to Project

```scala
// build.sbt
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % "0.23.x",
  "org.http4s" %% "http4s-ember-server" % "0.23.x",
  "org.http4s" %% "http4s-ember-client" % "0.23.x",
  "org.http4s" %% "http4s-circe" % "0.23.x"
)
```

### Step 2: Create http4s Server

```scala
// code/api/http4s/Http4sServer.scala
package code.api.http4s

import cats.effect._
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s._

object Http4sServer {
  
  def start(port: Int = 8081): IO[Unit] = {
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(Port.fromInt(port).get)
      .withHttpApp(routes.orNotFound)
      .build
      .useForever
  }
  
  def routes = ???  // Define routes here
}
```

### Step 3: THE JETTY PROBLEM

**IMPORTANT:** If you start http4s from Lift's Bootstrap, it runs INSIDE Jetty's servlet container. This defeats the purpose of using http4s!

```
❌ WRONG APPROACH:
┌─────────────────────────────┐
│ Jetty Servlet Container     │
│  ├─ Lift (port 8080)        │
│  └─ http4s (port 8081)      │  ← Still requires Jetty!
└─────────────────────────────┘
```

**The Problem:**
- http4s would still require Jetty to run
- Can't remove servlet container later
- Defeats the goal of eliminating Jetty

### Step 3: CORRECT APPROACH - Separate Main

**Solution:** Start http4s as a STANDALONE server, NOT from Lift Bootstrap.

```
✓ CORRECT APPROACH:
┌──────────────────┐  ┌─────────────────┐
│ Jetty Container  │  │ http4s Server   │
│  └─ Lift         │  │  (standalone)   │
│  Port 8080       │  │  Port 8081      │
└──────────────────┘  └─────────────────┘
     Same JVM Process
```

#### Option A: Two Separate Processes (Simpler)

```scala
// Run Lift as usual
sbt "jetty:start"  // Port 8080

// Run http4s separately
sbt "runMain code.api.http4s.Http4sMain"  // Port 8081
```

**Deployment:**
```bash
# Start Lift/Jetty
java -jar obp-api-jetty.jar &

# Start http4s standalone
java -jar obp-api-http4s.jar &
```

**Pros:**
- Complete separation
- Easy to understand
- Can stop/restart independently
- No Jetty dependency for http4s

**Cons:**
- Two separate processes to manage
- Two JVMs (more memory)

#### Option B: Single JVM with Two Threads (Complex but Doable)

```scala
// Main.scala - Entry point
object Main extends IOApp {
  
  def run(args: List[String]): IO[ExitCode] = {
    for {
      // Start Lift/Jetty in background fiber
      liftFiber <- startLiftServer().start
      
      // Start http4s server (blocks main thread)
      _ <- Http4sServer.start(8081)
    } yield ExitCode.Success
  }
  
  def startLiftServer(): IO[Unit] = IO {
    // Start Jetty programmatically
    val server = new Server(8080)
    val context = new WebAppContext()
    context.setContextPath("/")
    context.setWar("src/main/webapp")
    server.setHandler(context)
    server.start()
    // Don't call server.join() - let it run in background
  }
}
```

**Pros:**
- Single process
- Shared JVM, less memory
- Shared resources easier

**Cons:**
- More complex startup
- Harder to debug
- Mixed responsibilities

#### Option C: Use Jetty for Both (Transition Strategy)

During migration, you CAN start http4s from Lift Bootstrap using a servlet adapter, but this is TEMPORARY:

```scala
// bootstrap/liftweb/Boot.scala
class Boot {
  def boot {
    // Existing Lift setup
    LiftRules.addToPackages("code")
    
    // Add http4s routes to Jetty (TEMPORARY)
    if (APIUtil.getPropsAsBoolValue("http4s.enabled", false)) {
      // Add http4s servlet on different context path
      val http4sServlet = new Http4sServlet[IO](http4sRoutes)
      LiftRules.context.addServlet(http4sServlet, "/http4s/*")
    }
  }
}
```

Access via:
- Lift: `http://localhost:8080/obp/v6.0.0/banks/...`
- http4s: `http://localhost:8080/http4s/obp/v6.0.0/banks/...`

**Pros:**
- Single port
- Easy during development

**Cons:**
- http4s still requires Jetty
- Can't remove servlet container
- Only for development/testing

### RECOMMENDED APPROACH

**For actual migration, use Option A (Two Separate Processes):**

1. **Keep Lift/Jetty running as-is** on port 8080
2. **Create standalone http4s server** on port 8081 with its own Main class
3. **Use reverse proxy** (nginx/HAProxy) to route requests
4. **Migrate endpoints one by one** to http4s
5. **Eventually remove Lift/Jetty** completely

```
Phase 1-3 (Migration):
┌─────────────┐
│   Nginx     │  Port 443
│   (Proxy)   │
└──────┬──────┘
       │
       ├──→ Jetty/Lift (Process 1)      Port 8080  ← Old endpoints
       └──→ http4s standalone (Process 2) Port 8081  ← New endpoints

Phase 4 (Complete):
┌─────────────┐
│   Nginx     │  Port 443
│   (Proxy)   │
└──────┬──────┘
       │
       └──→ http4s standalone            Port 8080  ← All endpoints
            (Jetty removed!)
```

**This way http4s is NEVER dependent on Jetty.**

### Step 4: Shared Business Logic

```scala
// Keep business logic separate from HTTP layer
package code.api.service

object UserService {
  // Pure business logic - no Lift or http4s dependencies
  def createUser(username: String, email: String, password: String): Box[User] = {
    // Implementation
  }
}

// Use in Lift endpoint
class LiftEndpoints extends RestHelper {
  serve("obp" / "v6.0.0" prefix) {
    case "users" :: Nil JsonPost json -> _ => {
      val result = UserService.createUser(...)
      // Return Lift response
    }
  }
}

// Use in http4s endpoint
class Http4sEndpoints[F[_]: Concurrent] {
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req @ POST -> Root / "obp" / "v6.0.0" / "users" =>
      val result = UserService.createUser(...)
      // Return http4s response
  }
}
```

## Migration Strategy

### Phase 1: Setup (Week 1-2)
- Add http4s dependencies
- Create http4s server infrastructure
- Start http4s on port 8081
- Keep all endpoints on Lift

### Phase 2: Convert New Endpoints (Week 3-8)
- All NEW endpoints go to http4s only
- Existing endpoints stay on Lift
- Share business logic between both

### Phase 3: Migrate Existing Endpoints (Month 3-6)
Priority order:
1. Simple GET endpoints (read-only, no sessions)
2. POST endpoints with simple authentication
3. Endpoints with complex authorization
4. Admin/management endpoints
5. OAuth/authentication endpoints (last)

### Phase 4: Deprecation (Month 7-9)
- Announce Lift endpoints deprecated
- Run both servers (port 8080 and 8081)
- Redirect/proxy 8080 -> 8081
- Update documentation

### Phase 5: Removal (Month 10-12)
- Remove Lift dependencies
- Remove Jetty dependency
- Single http4s server on port 8080
- No servlet container needed

## Request Routing During Migration

### Option A: Two Separate Ports
```
Clients → Load Balancer
           ├─→ Port 8080 (Lift) - Old endpoints
           └─→ Port 8081 (http4s) - New endpoints
```

**Pros:** 
- Simple, clear separation
- Easy to monitor which endpoints are migrated
- No risk of conflicts

**Cons:**
- Clients need to know which port to use
- Load balancer configuration needed

### Option B: Proxy Pattern
```
Clients → Port 8080 (Lift)
           ├─→ Handle locally (Lift endpoints)
           └─→ Proxy to 8081 (http4s endpoints)
```

**Pros:**
- Single port for clients
- Transparent migration
- No client changes needed

**Cons:**
- Additional latency for proxied requests
- More complex routing logic

### Option C: Reverse Proxy (Nginx/HAProxy)
```
Clients → Nginx (Port 443)
           ├─→ Port 8080 (Lift) - /api/v4.0.0/*
           └─→ Port 8081 (http4s) - /api/v6.0.0/*
```

**Pros:**
- Professional solution
- Fine-grained routing rules
- SSL termination
- Load balancing

**Cons:**
- Additional infrastructure component
- Configuration overhead

## Database Access Migration

### Current: Lift Mapper
```scala
class AuthUser extends MegaProtoUser[AuthUser] {
  // Mapper ORM
}
```

### During Migration: Keep Mapper
```scala
// Both Lift and http4s use Mapper
// No need to migrate DB layer immediately
import code.model.dataAccess.AuthUser

// In http4s endpoint
def getUser(id: String): IO[Option[User]] = IO {
  AuthUser.find(By(AuthUser.id, id)).map(_.toUser)
}
```

### Future: Replace Mapper (Optional)
```scala
// Use Doobie or Skunk
import doobie._
import doobie.implicits._

def getUser(id: String): ConnectionIO[Option[User]] = {
  sql"SELECT * FROM authuser WHERE id = $id"
    .query[User]
    .option
}
```

## Configuration

```properties
# props/default.props

# Enable http4s server
http4s.enabled=true
http4s.port=8081

# Lift/Jetty (keep running)
jetty.port=8080

# Migration mode
# - "dual" = both servers running
# - "http4s-only" = only http4s
migration.mode=dual
```

## Testing Strategy

### Test Both Implementations
```scala
class UserEndpointTest extends ServerSetup {
  
  // Test Lift version
  scenario("Create user via Lift (port 8080)") {
    val request = (v6_0_0_Request / "users").POST
    val response = makePostRequest(request, userJson)
    response.code should equal(201)
  }
  
  // Test http4s version  
  scenario("Create user via http4s (port 8081)") {
    val request = (http4s_v6_0_0_Request / "users").POST
    val response = makePostRequest(request, userJson)
    response.code should equal(201)
  }
  
  // Test both give same result
  scenario("Both implementations return same result") {
    val liftResult = makeLiftRequest(...)
    val http4sResult = makeHttp4sRequest(...)
    liftResult should equal(http4sResult)
  }
}
```

## Resource Docs Compatibility

### Keep Same ResourceDoc Structure
```scala
// Shared ResourceDoc definition
val createUserDoc = ResourceDoc(
  createUser,
  implementedInApiVersion,
  "createUser",
  "POST",
  "/users",
  "Create User",
  """Creates a new user...""",
  postUserJson,
  userResponseJson,
  List(UserNotLoggedIn, InvalidJsonFormat)
)

// Lift endpoint references it
lazy val createUserLift: OBPEndpoint = {
  case "users" :: Nil JsonPost json -> _ => {
    // implementation
  }
}

// http4s endpoint references same doc
def createUserHttp4s[F[_]: Concurrent]: HttpRoutes[F] = {
  case req @ POST -> Root / "users" => {
    // implementation  
  }
}
```

## Advantages of Coexistence Approach

1. **Zero Downtime Migration**
   - Old endpoints keep working
   - New endpoints added incrementally
   - No big-bang rewrite

2. **Risk Mitigation**
   - Test new framework alongside old
   - Easy rollback per endpoint
   - Gradual learning curve

3. **Business Continuity**
   - No disruption to users
   - Features can still be added
   - Migration in background

4. **Shared Resources**
   - Same database
   - Same business logic
   - Same configuration

5. **Flexible Timeline**
   - Migrate at your own pace
   - Pause if needed
   - No hard deadlines

## Challenges and Solutions

### Challenge 1: Port Management
**Solution:** Use property files to configure ports, allow override

### Challenge 2: Session/State Sharing
**Solution:** Use stateless JWT tokens, shared Redis for sessions

### Challenge 3: Authentication
**Solution:** Keep auth logic separate, callable from both frameworks

### Challenge 4: Database Connections
**Solution:** Shared connection pool, configure max connections appropriately

### Challenge 5: Monitoring
**Solution:** Separate metrics for each server, aggregate in monitoring system

### Challenge 6: Deployment
**Solution:** Single JAR with both servers, configure which to start

## Deployment Considerations

### Development
```bash
# Start with both servers
sbt run
# Lift on :8080, http4s on :8081
```

### Production - Transition Period
```
# Run both servers
java -Dhttp4s.enabled=true \
     -Dhttp4s.port=8081 \
     -Djetty.port=8080 \
     -jar obp-api.jar
```

### Production - After Migration
```
# Only http4s
java -Dhttp4s.enabled=true \
     -Dhttp4s.port=8080 \
     -Dlift.enabled=false \
     -jar obp-api.jar
```

## Example: First Endpoint Migration

### 1. Existing Lift Endpoint
```scala
// APIMethods600.scala
lazy val getBank: OBPEndpoint = {
  case "banks" :: bankId :: Nil JsonGet _ => {
    cc => implicit val ec = EndpointContext(Some(cc))
    for {
      bank <- Future { Connector.connector.vend.getBank(BankId(bankId)) }
    } yield {
      (bank, HttpCode.`200`(cc))
    }
  }
}
```

### 2. Create http4s Version
```scala
// code/api/http4s/endpoints/BankEndpoints.scala
class BankEndpoints[F[_]: Concurrent] {
  
  def routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "obp" / "v6.0.0" / "banks" / bankId =>
      // Same business logic
      val bankBox = Connector.connector.vend.getBank(BankId(bankId))
      
      bankBox match {
        case Full(bank) => Ok(bank.toJson)
        case Empty => NotFound()
        case Failure(msg, _, _) => BadRequest(msg)
      }
  }
}
```

### 3. Both Available
- Lift: `http://localhost:8080/obp/v6.0.0/banks/{bankId}`
- http4s: `http://localhost:8081/obp/v6.0.0/banks/{bankId}`

### 4. Test Both
```scala
scenario("Get bank - Lift version") {
  val response = makeGetRequest(v6_0_0_Request / "banks" / testBankId.value)
  response.code should equal(200)
}

scenario("Get bank - http4s version") {
  val response = makeGetRequest(http4s_v6_0_0_Request / "banks" / testBankId.value)
  response.code should equal(200)
}
```

### 5. Deprecate Lift Version
- Add deprecation warning to Lift endpoint
- Update docs to point to http4s version
- Monitor usage

### 6. Remove Lift Version
- Delete Lift endpoint code
- All traffic to http4s

## Timeline Estimate

### Conservative Approach (12-18 months)
- **Month 1-2:** Setup and infrastructure
- **Month 3-6:** Migrate 25% of endpoints
- **Month 7-10:** Migrate 50% more (75% total)
- **Month 11-14:** Migrate remaining 25%
- **Month 15-16:** Testing and stabilization
- **Month 17-18:** Remove Lift, cleanup

### Aggressive Approach (6-9 months)
- **Month 1:** Setup
- **Month 2-5:** Migrate 80% of endpoints
- **Month 6-7:** Migrate remaining 20%
- **Month 8-9:** Remove Lift

## Conclusion

**Yes, Lift and http4s can coexist** by running on different ports (8080 and 8081) within the same application. This allows for:

- Gradual, low-risk migration
- Endpoint-by-endpoint conversion
- Shared business logic and resources
- Zero downtime
- Flexible timeline

The key is to **keep HTTP layer separate from business logic** so both frameworks can call the same underlying functions.

Start with simple read-only endpoints, then gradually migrate more complex ones, finally removing Lift when all endpoints are converted.