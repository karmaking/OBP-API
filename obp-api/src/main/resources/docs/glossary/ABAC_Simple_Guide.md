# ABAC Rules Engine - Simple Guide

## Overview

The ABAC (Attribute-Based Access Control) Rules Engine allows you to create dynamic access control rules in Scala that evaluate whether a user should have access to a resource.

## Core Concept

**One Rule + One Execution Method = Simple Access Control**

```scala
def executeRule(
  ruleId: String,
  authenticatedUserId: String,
  onBehalfOfUserId: Option[String] = None,
  userId: Option[String] = None,
  callContext: Option[CallContext] = None,
  bankId: Option[String] = None,
  accountId: Option[String] = None,
  viewId: Option[String] = None,
  transactionId: Option[String] = None,
  customerId: Option[String] = None
): Box[Boolean]
```

---

## Understanding the Three User Parameters

### 1. `authenticatedUserId` (Required)
**The person actually logged in and making the API call**

- This is ALWAYS the real user who authenticated
- Retrieved from the authentication token
- Cannot be faked or changed

**Example:** Alice logs into the banking app
- `authenticatedUserId = "alice@example.com"`

---

### 2. `onBehalfOfUserId` (Optional)
**When someone acts on behalf of another user (delegation)**

- Used for delegation scenarios
- The authenticated user is acting for someone else
- Common in customer service, admin tools, power of attorney

**Example:** Customer service rep Bob helps Alice with her account
- `authenticatedUserId = "bob@customerservice.com"` (the rep logged in)
- `onBehalfOfUserId = "alice@example.com"` (helping Alice)
- `userId = "alice@example.com"` (checking Alice's permissions)

---

### 3. `userId` (Optional)
**The target user being evaluated by the rule**

- Defaults to `authenticatedUserId` if not provided
- The user whose permissions/attributes are being checked
- Useful for testing rules for different users

**Example:** Admin checking if Alice can access an account
- `authenticatedUserId = "admin@example.com"` (admin is logged in)
- `userId = "alice@example.com"` (checking Alice's access)

---

## Common Scenarios

### Scenario 1: Normal User Access
**Alice wants to view her own account**

```json
{
  "bank_id": "gh.29.uk",
  "account_id": "alice-account-123"
}
```

Behind the scenes:
- `authenticatedUserId = "alice@example.com"` (from auth token)
- `onBehalfOfUserId = None`
- `userId = None` → defaults to Alice

**Rule example:**
```scala
// Check if user owns the account
accountOpt.exists(account => 
  account.owners.exists(owner => owner.userId == user.userId)
)
```

---

### Scenario 2: Customer Service Delegation
**Bob (customer service) helps Alice view her account**

```json
{
  "on_behalf_of_user_id": "alice@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "alice-account-123"
}
```

Behind the scenes:
- `authenticatedUserId = "bob@customerservice.com"` (from auth token)
- `onBehalfOfUserId = "alice@example.com"`
- `userId = None` → defaults to Bob, but rule can check both

**Rule example:**
```scala
// Allow if authenticated user is customer service AND acting on behalf of an account owner
val isCustomerService = authenticatedUser.emailAddress.contains("@customerservice.com")
val hasValidDelegation = onBehalfOfUserOpt.isDefined
val targetOwnsAccount = accountOpt.exists(account =>
  account.owners.exists(owner => owner.userId == user.userId)
)

isCustomerService && hasValidDelegation && targetOwnsAccount
```

---

### Scenario 3: Admin Testing
**Admin wants to test if Alice can access an account (without logging in as Alice)**

```json
{
  "user_id": "alice@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "alice-account-123"
}
```

Behind the scenes:
- `authenticatedUserId = "admin@example.com"` (from auth token)
- `onBehalfOfUserId = None`
- `userId = "alice@example.com"` (evaluating for Alice)

**Rule example:**
```scala
// Allow admins to test access, or allow if user owns account
val isAdmin = authenticatedUser.emailAddress.endsWith("@admin.com")
val userOwnsAccount = accountOpt.exists(account =>
  account.owners.exists(owner => owner.userId == user.userId)
)

isAdmin || userOwnsAccount
```

---

## API Usage

### Endpoint
```
POST /obp/v6.0.0/management/abac-rules/{RULE_ID}/execute
```

### Request Examples

#### Example 1: Basic Access Check
```json
{
  "bank_id": "gh.29.uk",
  "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
}
```
- Checks if authenticated user can access the account

#### Example 2: Delegation
```json
{
  "on_behalf_of_user_id": "alice@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
}
```
- Authenticated user acting on behalf of Alice

#### Example 3: Testing for Different User
```json
{
  "user_id": "bob@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
}
```
- Check if Bob can access the account (useful for admins testing)

#### Example 4: Complex Scenario
```json
{
  "on_behalf_of_user_id": "alice@example.com",
  "user_id": "charlie@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
  "transaction_id": "trans-123"
}
```
- Authenticated user acting on behalf of Alice
- Checking if Charlie can access account and transaction

---

## Writing ABAC Rules

### Available Objects in Rules

```scala
// These are available in your rule code:
authenticatedUser: User                    // Always present - the logged in user
onBehalfOfUserOpt: Option[User]           // Present if delegation
user: User                                 // Always present - the target user being evaluated
bankOpt: Option[Bank]                      // Present if bank_id provided
accountOpt: Option[BankAccount]            // Present if account_id provided
transactionOpt: Option[Transaction]        // Present if transaction_id provided
customerOpt: Option[Customer]              // Present if customer_id provided
```

### Simple Rule Examples

#### Rule 1: User Must Own Account
```scala
accountOpt.exists(account => 
  account.owners.exists(owner => owner.userId == user.userId)
)
```

#### Rule 2: Admin or Owner
```scala
val isAdmin = authenticatedUser.emailAddress.endsWith("@admin.com")
val isOwner = accountOpt.exists(account =>
  account.owners.exists(owner => owner.userId == user.userId)
)

isAdmin || isOwner
```

#### Rule 3: Customer Service Delegation
```scala
val isCustomerService = authenticatedUser.emailAddress.contains("@customerservice.com")
val actingOnBehalf = onBehalfOfUserOpt.isDefined
val userIsOwner = accountOpt.exists(account =>
  account.owners.exists(owner => owner.userId == user.userId)
)

// Allow if customer service is helping an account owner
isCustomerService && actingOnBehalf && userIsOwner
```

#### Rule 4: Self-Service Only (No Delegation)
```scala
// User must be checking their own access (no delegation allowed)
val isSelfService = authenticatedUser.userId == user.userId
val noDelegation = onBehalfOfUserOpt.isEmpty

isSelfService && noDelegation
```

#### Rule 5: Account Balance Check
```scala
accountOpt.exists(account => account.balance.toDouble >= 1000.0)
```

---

## Quick Reference Table

| Parameter | Required? | Purpose | Example Value |
|-----------|-----------|---------|---------------|
| `authenticatedUserId` | ✅ Yes | Who is logged in | `"alice@example.com"` |
| `onBehalfOfUserId` | ❌ Optional | Delegation | `"bob@example.com"` |
| `userId` | ❌ Optional | Target user to evaluate | `"charlie@example.com"` |
| `bankId` | ❌ Optional | Bank context | `"gh.29.uk"` |
| `accountId` | ❌ Optional | Account context | `"acc-123"` |
| `viewId` | ❌ Optional | View context | `"owner"` |
| `transactionId` | ❌ Optional | Transaction context | `"trans-456"` |
| `customerId` | ❌ Optional | Customer context | `"cust-789"` |

---

## Real-World Use Cases

### Use Case 1: Personal Banking
- User logs in → `authenticatedUserId`
- Views their own account → `userId` defaults to authenticated user
- Rule checks ownership

### Use Case 2: Business Banking with Delegates
- CFO logs in → `authenticatedUserId = "cfo@company.com"`
- Checks on behalf of CEO → `onBehalfOfUserId = "ceo@company.com"`
- System evaluates if CEO has access → `userId = "ceo@company.com"`

### Use Case 3: Customer Support
- Support agent logs in → `authenticatedUserId = "agent@bank.com"`
- Helps customer → `onBehalfOfUserId = "customer@example.com"`
- Rule verifies: agent has support role AND customer owns account

### Use Case 4: Admin Panel
- Admin logs in → `authenticatedUserId = "admin@bank.com"`
- Tests rule for any user → `userId = "testuser@example.com"`
- Rule evaluates for test user, but admin must be authenticated

---

## Testing Tips

### Test Different Users
```bash
# Test as yourself
curl -X POST .../execute -d '{"bank_id": "gh.29.uk"}'

# Test for another user (if you have permission)
curl -X POST .../execute -d '{"user_id": "other@example.com", "bank_id": "gh.29.uk"}'
```

### Test Delegation
```bash
# Act on behalf of someone
curl -X POST .../execute -d '{
  "on_behalf_of_user_id": "alice@example.com",
  "bank_id": "gh.29.uk"
}'
```

### Debug Your Rules
```scala
// Add simple checks to understand what's happening
val result = (authenticatedUser.userId == user.userId)
println(s"Auth user: ${authenticatedUser.userId}, Target user: ${user.userId}, Match: $result")
result
```

---

## Summary

✅ **Keep it simple**: One execution method, clear parameters
✅ **Three user IDs**: authenticated (who), on-behalf-of (delegation), user (target)
✅ **Write rules in Scala**: Full power of the language
✅ **Test via API**: Just pass IDs, objects fetched automatically
✅ **Flexible**: Supports normal access, delegation, and admin testing

---

**Related Documentation:**
- `ABAC_OBJECT_PROPERTIES_REFERENCE.md` - Full list of available properties
- `ABAC_TESTING_EXAMPLES.md` - More testing examples
- `ABAC_REFACTORING.md` - Technical implementation details

**Last Updated:** 2024