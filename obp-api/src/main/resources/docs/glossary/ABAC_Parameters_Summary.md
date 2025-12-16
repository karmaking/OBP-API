# ABAC Rule Parameters - Complete Reference

This document lists all 16 parameters available in ABAC (Attribute-Based Access Control) rules.

## Overview

ABAC rules receive **18 parameters** that provide complete context for access control decisions.

**All parameters are READ-ONLY** - you can only read and evaluate, never modify.

## Complete Parameter List

| # | Parameter | Type | Always Available? | Description |
|---|-----------|------|-------------------|-------------|
| 1 | `authenticatedUser` | `User` | ✅ Yes | The user who is logged in and making the API call |
| 2 | `authenticatedUserAttributes` | `List[UserAttribute]` | ✅ Yes | Non-personal attributes for the authenticated user (may be empty) |
| 3 | `authenticatedUserAuthContext` | `List[UserAuthContext]` | ✅ Yes | Auth context for the authenticated user (may be empty) |
| 4 | `onBehalfOfUserOpt` | `Option[User]` | ❌ Optional | User being represented in delegation scenarios |
| 5 | `onBehalfOfUserAttributes` | `List[UserAttribute]` | ✅ Yes | Non-personal attributes for onBehalfOf user (empty if no delegation) |
| 6 | `onBehalfOfUserAuthContext` | `List[UserAuthContext]` | ✅ Yes | Auth context for onBehalfOf user (empty if no delegation) |
| 7 | `userOpt` | `Option[User]` | ❌ Optional | A user object (when user_id is provided) |
| 8 | `userAttributes` | `List[UserAttribute]` | ✅ Yes | Non-personal attributes for user (empty if no user) |
| 9 | `bankOpt` | `Option[Bank]` | ❌ Optional | Bank object (when bank_id is provided) |
| 10 | `bankAttributes` | `List[BankAttributeTrait]` | ✅ Yes | Attributes for bank (empty if no bank) |
| 11 | `accountOpt` | `Option[BankAccount]` | ❌ Optional | Account object (when account_id is provided) |
| 12 | `accountAttributes` | `List[AccountAttribute]` | ✅ Yes | Attributes for account (empty if no account) |
| 13 | `transactionOpt` | `Option[Transaction]` | ❌ Optional | Transaction object (when transaction_id is provided) |
| 14 | `transactionAttributes` | `List[TransactionAttribute]` | ✅ Yes | Attributes for transaction (empty if no transaction) |
| 15 | `transactionRequestOpt` | `Option[TransactionRequest]` | ❌ Optional | Transaction request object (when transaction_request_id is provided) |
| 16 | `transactionRequestAttributes` | `List[TransactionRequestAttributeTrait]` | ✅ Yes | Attributes for transaction request (empty if no transaction request) |
| 17 | `customerOpt` | `Option[Customer]` | ❌ Optional | Customer object (when customer_id is provided) |
| 18 | `customerAttributes` | `List[CustomerAttribute]` | ✅ Yes | Attributes for customer (empty if no customer) |

## Function Signature

```scala
type AbacRuleFunction = (
  User,                              // 1. authenticatedUser
  List[UserAttribute],               // 2. authenticatedUserAttributes
  List[UserAuthContext],             // 3. authenticatedUserAuthContext
  Option[User],                      // 4. onBehalfOfUserOpt
  List[UserAttribute],               // 5. onBehalfOfUserAttributes
  List[UserAuthContext],             // 6. onBehalfOfUserAuthContext
  Option[User],                      // 7. userOpt
  List[UserAttribute],               // 8. userAttributes
  Option[Bank],                      // 9. bankOpt
  List[BankAttributeTrait],          // 10. bankAttributes
  Option[BankAccount],               // 11. accountOpt
  List[AccountAttribute],            // 12. accountAttributes
  Option[Transaction],               // 13. transactionOpt
  List[TransactionAttribute],        // 14. transactionAttributes
  Option[TransactionRequest],        // 15. transactionRequestOpt
  List[TransactionRequestAttributeTrait], // 16. transactionRequestAttributes
  Option[Customer],                  // 17. customerOpt
  List[CustomerAttribute]            // 18. customerAttributes
) => Boolean
```

## Parameter Groups

### Group 1: Authenticated User (Always Available)
- `authenticatedUser` - The logged in user
- `authenticatedUserAttributes` - Their non-personal attributes
- `authenticatedUserAuthContext` - Their auth context (session, IP, etc.)

### Group 2: OnBehalfOf User (Delegation)
- `onBehalfOfUserOpt` - Optional delegated user
- `onBehalfOfUserAttributes` - Their non-personal attributes (empty if no delegation)
- `onBehalfOfUserAuthContext` - Their auth context (empty if no delegation)

### Group 3: Target User (Optional)
- `userOpt` - Optional user object
- `userAttributes` - Their non-personal attributes (empty if no user)

### Group 4: Bank (Optional)
- `bankOpt` - Optional bank object
- `bankAttributes` - Bank attributes (empty if no bank)

### Group 5: Account (Optional)
- `accountOpt` - Optional account object
- `accountAttributes` - Account attributes (empty if no account)

### Group 6: Transaction (Optional)
- `transactionOpt` - Optional transaction object
- `transactionAttributes` - Transaction attributes (empty if no transaction)

### Group 7: Transaction Request (Optional)
- `transactionRequestOpt` - Optional transaction request object
- `transactionRequestAttributes` - Transaction request attributes (empty if no transaction request)

### Group 8: Customer (Optional)
- `customerOpt` - Optional customer object
- `customerAttributes` - Customer attributes (empty if no customer)

## Example Rules

### Example 1: Check Authenticated User Attribute
```scala
authenticatedUserAttributes.exists(attr => 
  attr.name == "department" && attr.value == "finance"
)
```

### Example 2: Check Bank Attribute
```scala
bankAttributes.exists(attr =>
  attr.name == "country" && attr.value == "UK"
)
```

### Example 3: Check Account Attribute
```scala
accountAttributes.exists(attr =>
  attr.name == "account_type" && attr.value == "premium"
)
```

### Example 4: Check Transaction Attribute
```scala
transactionAttributes.exists(attr =>
  attr.name == "risk_score" && 
  attr.value.toIntOption.exists(_ < 5)
)
```

### Example 5: Check Transaction Request Attribute
```scala
transactionRequestAttributes.exists(attr =>
  attr.name == "approval_status" && attr.value == "pending"
)
```

### Example 6: Check Customer Attribute
```scala
customerAttributes.exists(attr =>
  attr.name == "kyc_status" && attr.value == "verified"
)
```

### Example 7: Complex Multi-Attribute Rule
```scala
// Allow if:
// - Authenticated user is in finance department
// - Bank is in allowed countries
// - Account is premium
// - Transaction risk is low

val authIsFinance = authenticatedUserAttributes.exists(attr =>
  attr.name == "department" && attr.value == "finance"
)

val bankAllowed = bankAttributes.exists(attr =>
  attr.name == "country" && List("UK", "US", "DE").contains(attr.value)
)

val accountPremium = accountAttributes.exists(attr =>
  attr.name == "account_type" && attr.value == "premium"
)

val lowRisk = transactionAttributes.exists(attr =>
  attr.name == "risk_score" && attr.value.toIntOption.exists(_ < 3)
)

authIsFinance && bankAllowed && accountPremium && lowRisk
```

### Example 8: Delegation with Attributes
```scala
// Allow customer service to help premium customers
val isCustomerService = authenticatedUserAttributes.exists(attr =>
  attr.name == "role" && attr.value == "customer_service"
)

val hasDelegation = onBehalfOfUserOpt.isDefined

val customerIsPremium = onBehalfOfUserAttributes.exists(attr =>
  attr.name == "customer_tier" && attr.value == "premium"
)

isCustomerService && hasDelegation && customerIsPremium
```

## API Request Mapping

When you make an API request:

```json
{
  "authenticated_user_id": "alice@example.com",
  "on_behalf_of_user_id": "bob@example.com",
  "user_id": "charlie@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "acc-123",
  "transaction_id": "txn-456",
  "transaction_request_id": "tr-123",
  "customer_id": "cust-789"
}
```

The engine automatically:
1. Fetches `authenticatedUser` using `authenticated_user_id` (or from auth token if not provided)
2. Fetches `authenticatedUserAttributes` and `authenticatedUserAuthContext` for authenticated user
3. Fetches `onBehalfOfUserOpt`, `onBehalfOfUserAttributes`, `onBehalfOfUserAuthContext` if `on_behalf_of_user_id` provided
4. Fetches `userOpt` and `userAttributes` if `user_id` provided
5. Fetches `bankOpt` and `bankAttributes` if `bank_id` provided
6. Fetches `accountOpt` and `accountAttributes` if `account_id` provided
7. Fetches `transactionOpt` and `transactionAttributes` if `transaction_id` provided
8. Fetches `transactionRequestOpt` and `transactionRequestAttributes` if `transaction_request_id` provided
9. Fetches `customerOpt` and `customerAttributes` if `customer_id` provided

## Working with Attributes

All attribute lists follow the same pattern:

```scala
// Check if attribute exists with specific value
attributeList.exists(attr => attr.name == "key" && attr.value == "value")

// Check if list is empty
attributeList.isEmpty

// Check if list has any attributes
attributeList.nonEmpty

// Find specific attribute
attributeList.find(_.name == "key").map(_.value)

// Multiple attributes (AND)
val hasAttr1 = attributeList.exists(_.name == "key1")
val hasAttr2 = attributeList.exists(_.name == "key2")
hasAttr1 && hasAttr2

// Multiple attributes (OR)
attributeList.exists(attr => 
  List("key1", "key2", "key3").contains(attr.name)
)
```

## Key Points

✅ **18 parameters total** - comprehensive context for access decisions
✅ **3 always available objects** - authenticatedUser, authenticatedUserAttributes, authenticatedUserAuthContext
✅ **15 contextual parameters** - available based on what IDs are provided in the request
✅ **All READ-ONLY** - cannot modify any parameter values
✅ **Automatic fetching** - engine fetches all data based on provided IDs
✅ **Type safety** - optional objects use `Option[T]`, lists are `List[T]`
✅ **Empty lists not None** - attribute lists are always available, just empty when no data

## Summary

ABAC rules have access to:
- **3 user contexts**: authenticated, onBehalfOf, and target user
- **5 resource contexts**: bank, account, transaction, transaction request, customer
- **Complete attribute data**: for all users and resources
- **Auth context**: session, IP, device info, etc.
- **Full type safety**: optional objects and guaranteed lists

This provides everything needed to make sophisticated access control decisions!

---

**Related Documentation:**
- `ABAC_OBJECT_PROPERTIES_REFERENCE.md` - Detailed property reference for each object
- `ABAC_SIMPLE_GUIDE.md` - Getting started guide
- `ABAC_REFACTORING.md` - Technical implementation details

**Last Updated:** 2024