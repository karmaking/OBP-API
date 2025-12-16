# ABAC Rule Object Properties Reference

This document provides a comprehensive reference for all properties available on objects that can be used in ABAC (Attribute-Based Access Control) rules.

## Overview

When you write ABAC rules, you have access to eleven objects:

1. **authenticatedUser** - The authenticated user making the API call (always available)
2. **authenticatedUserAttributes** - Non-personal attributes for the authenticated user (always available)
3. **authenticatedUserAuthContext** - Auth context for the authenticated user (always available)
4. **onBehalfOfUserOpt** - Optional user for delegation scenarios
5. **onBehalfOfUserAttributes** - Non-personal attributes for the onBehalfOf user (always available, may be empty)
6. **onBehalfOfUserAuthContext** - Auth context for the onBehalfOf user (always available, may be empty)
7. **user** - A user object (always available)
8. **bankOpt** - Optional bank context
9. **accountOpt** - Optional account context
10. **transactionOpt** - Optional transaction context
11. **customerOpt** - Optional customer context

**Important: All objects are READ-ONLY.** You cannot modify user attributes, auth context, or any other objects within ABAC rules.

## How to Use This Reference

When writing ABAC rules, you can access properties using dot notation:

```scala
// Example: Check if authenticated user is admin
authenticatedUser.emailAddress.endsWith("@admin.com")

// Example: Check authenticated user attributes
authenticatedUserAttributes.exists(attr => attr.name == "department" && attr.value == "finance")

// Example: Check authenticated user auth context
authenticatedUserAuthContext.exists(ctx => ctx.key == "session_id")

// Example: Check if delegation is present
onBehalfOfUserOpt.isDefined

// Example: Check onBehalfOf user attributes
onBehalfOfUserAttributes.exists(attr => attr.name == "role" && attr.value == "manager")

// Example: Check onBehalfOf user auth context
onBehalfOfUserAuthContext.exists(ctx => ctx.key == "device_id")

// Example: Check if user has specific email
user.emailAddress == "alice@example.com"

// Example: Check if account balance is above 1000
accountOpt.exists(account => account.balance.toDouble > 1000.0)

// Example: Check if bank is in UK
bankOpt.exists(bank => bank.bankId.value.startsWith("gh."))
```

---

## 1. authenticatedUser (User)

The authenticated user making the API call. This is always available (not optional).

### Available Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `userId` | `String` | Unique UUID for the user | `"f47ac10b-58cc-4372-a567-0e02b2c3d479"` |
| `idGivenByProvider` | `String` | Same as username | `"alice@example.com"` |
| `provider` | `String` | Authentication provider | `"obp"`, `"oauth"`, `"openid"` |
| `emailAddress` | `String` | User's email address | `"alice@example.com"` |
| `name` | `String` | User's full name | `"Alice Smith"` |
| `createdByConsentId` | `Option[String]` | Consent ID if user created via consent | `Some("consent-123")` or `None` |
| `createdByUserInvitationId` | `Option[String]` | User invitation ID if applicable | `Some("invite-456")` or `None` |
| `isDeleted` | `Option[Boolean]` | Whether user is deleted | `Some(false)` or `None` |
| `lastMarketingAgreementSignedDate` | `Option[Date]` | Last marketing agreement date | `Some(Date)` or `None` |
| `lastUsedLocale` | `Option[String]` | Last used locale/language | `Some("en_GB")` or `None` |

### Helper Methods

| Method | Type | Description |
|--------|------|-------------|
| `isOriginalUser` | `Boolean` | True if user created by OBP (not via consent) |
| `isConsentUser` | `Boolean` | True if user created via consent |

### Example Rules Using authenticatedUser

```scala
// 1. Allow only admin users (by email suffix)
authenticatedUser.emailAddress.endsWith("@admin.com")

// 2. Allow specific user by ID
authenticatedUser.userId == "f47ac10b-58cc-4372-a567-0e02b2c3d479"

// 3. Allow only original users (not consent users)
authenticatedUser.isOriginalUser

// 4. Check if user has name
authenticatedUser.name.nonEmpty

// 5. Check authentication provider
authenticatedUser.provider == "obp"

// 6. Complex condition
authenticatedUser.emailAddress.endsWith("@admin.com") || 
authenticatedUser.name.contains("Manager")
```

---

## 2. authenticatedUserAttributes (List[UserAttribute])

Non-personal attributes for the authenticated user. This is always available (not optional), but may be an empty list.

### UserAttribute Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `userAttributeId` | `String` | Unique attribute ID | `"attr-123"` |
| `userId` | `String` | User ID this attribute belongs to | `"user-456"` |
| `name` | `String` | Attribute name | `"department"`, `"role"`, `"clearance_level"` |
| `attributeType` | `UserAttributeType.Value` | Type of attribute | `UserAttributeType.STRING`, `UserAttributeType.INTEGER` |
| `value` | `String` | Attribute value | `"finance"`, `"manager"`, `"5"` |
| `insertDate` | `Date` | When attribute was created | `Date(...)` |
| `isPersonal` | `Boolean` | Whether attribute is personal (always false here) | `false` |

### Example Rules Using authenticatedUserAttributes

```scala
// 1. Check if user has a specific attribute
authenticatedUserAttributes.exists(attr => 
  attr.name == "department" && attr.value == "finance"
)

// 2. Check if user has clearance level >= 3
authenticatedUserAttributes.exists(attr =>
  attr.name == "clearance_level" && 
  attr.value.toIntOption.exists(_ >= 3)
)

// 3. Check if user has any attributes
authenticatedUserAttributes.nonEmpty

// 4. Check multiple attributes (AND)
val hasDepartment = authenticatedUserAttributes.exists(_.name == "department")
val hasRole = authenticatedUserAttributes.exists(_.name == "role")
hasDepartment && hasRole

// 5. Get specific attribute value
val departmentOpt = authenticatedUserAttributes.find(_.name == "department").map(_.value)
departmentOpt.contains("finance")

// 6. Check attribute with multiple possible values (OR)
authenticatedUserAttributes.exists(attr =>
  attr.name == "role" && 
  List("admin", "manager", "supervisor").contains(attr.value)
)

// 7. Combine with user properties
authenticatedUser.emailAddress.endsWith("@admin.com") ||
authenticatedUserAttributes.exists(attr => attr.name == "admin_override" && attr.value == "true")
```

---

## 3. authenticatedUserAuthContext (List[UserAuthContext])

Authentication context for the authenticated user. This is always available (not optional), but may be an empty list.

**READ-ONLY:** These values cannot be modified within ABAC rules.

### UserAuthContext Properties

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `userAuthContextId` | `String` | Unique auth context ID | `"ctx-123"` |
| `userId` | `String` | User ID this context belongs to | `"user-456"` |
| `key` | `String` | Context key | `"session_id"`, `"ip_address"`, `"device_id"` |
| `value` | `String` | Context value | `"sess-abc-123"`, `"192.168.1.1"`, `"device-xyz"` |
| `timeStamp` | `Date` | When context was created | `Date(...)` |
| `consumerId` | `String` | Consumer/app that created this context | `"consumer-789"` |

### Example Rules Using authenticatedUserAuthContext

```scala
// 1. Check if user has a specific auth context
authenticatedUserAuthContext.exists(ctx => 
  ctx.key == "ip_address" && ctx.value.startsWith("192.168.")
)

// 2. Check if session exists
authenticatedUserAuthContext.exists(ctx => ctx.key == "session_id")

// 3. Check if auth context was recently created (within last hour)
import java.time.Instant
import java.time.temporal.ChronoUnit

authenticatedUserAuthContext.exists(ctx => {
  val now = Instant.now()
  val ctxInstant = ctx.timeStamp.toInstant
  ChronoUnit.HOURS.between(ctxInstant, now) < 1
})

// 4. Check multiple context values (AND)
val hasSession = authenticatedUserAuthContext.exists(_.key == "session_id")
val hasDevice = authenticatedUserAuthContext.exists(_.key == "device_id")
hasSession && hasDevice

// 5. Get specific context value
val ipAddressOpt = authenticatedUserAuthContext.find(_.key == "ip_address").map(_.value)
ipAddressOpt.exists(ip => ip.startsWith("10.0."))

// 6. Check consumer ID
authenticatedUserAuthContext.exists(ctx =>
  ctx.consumerId == "trusted-consumer-123"
)

// 7. Combine with user properties
authenticatedUser.emailAddress.endsWith("@admin.com") &&
authenticatedUserAuthContext.exists(_.key == "mfa_verified" && _.value == "true")
```

---

## 4. onBehalfOfUserOpt (Option[User])

Optional user for delegation scenarios. Present when someone acts on behalf of another user.

This is an `Option[User]` - use `.exists()`, `.isDefined`, `.isEmpty`, or pattern matching.

### Available Properties (when present)

Same properties as `authenticatedUser` (see section 1 above).

**Note:** When `onBehalfOfUserOpt` is present, the corresponding `onBehalfOfUserAttributes` and `onBehalfOfUserAuthContext` lists will contain data for that user.

### Example Rules Using onBehalfOfUserOpt

```scala
// 1. Check if delegation is being used
onBehalfOfUserOpt.isDefined

// 2. Check if no delegation (direct access only)
onBehalfOfUserOpt.isEmpty

// 3. Check delegation user's email
onBehalfOfUserOpt.exists(delegatedUser => 
  delegatedUser.emailAddress.endsWith("@company.com")
)

// 4. Allow if authenticated user is customer service AND delegation is used
val isCustomerService = authenticatedUser.emailAddress.contains("@customerservice.com")
val hasDelegation = onBehalfOfUserOpt.isDefined
isCustomerService && hasDelegation

// 5. Check both authenticated and delegation users
onBehalfOfUserOpt match {
  case Some(delegatedUser) =>
    authenticatedUser.emailAddress.endsWith("@admin.com") &&
    delegatedUser.emailAddress.nonEmpty
  case None => true // No delegation, allow
}
```

---

## 5. onBehalfOfUserAttributes (List[UserAttribute])

Non-personal attributes for the onBehalfOf user. This is always available (not optional), but will be an empty list if no delegation is happening.

**READ-ONLY:** These values cannot be modified within ABAC rules.

### UserAttribute Properties

Same properties as `authenticatedUserAttributes` (see section 2 above).

### Example Rules Using onBehalfOfUserAttributes

```scala
// 1. Check if onBehalfOf user has specific attribute
onBehalfOfUserAttributes.exists(attr => 
  attr.name == "department" && attr.value == "sales"
)

// 2. Check if onBehalfOf user has attributes (delegation with data)
onBehalfOfUserAttributes.nonEmpty

// 3. Verify delegation user has required role
onBehalfOfUserOpt.isDefined &&
onBehalfOfUserAttributes.exists(attr =>
  attr.name == "role" && attr.value == "manager"
)

// 4. Compare authenticated and onBehalfOf user departments
val authDept = authenticatedUserAttributes.find(_.name == "department").map(_.value)
val onBehalfDept = onBehalfOfUserAttributes.find(_.name == "department").map(_.value)
authDept == onBehalfDept

// 5. Check clearance level for delegation
onBehalfOfUserAttributes.exists(attr =>
  attr.name == "clearance_level" && 
  attr.value.toIntOption.exists(_ >= 2)
)
```

---

## 6. onBehalfOfUserAuthContext (List[UserAuthContext])

Authentication context for the onBehalfOf user. This is always available (not optional), but will be an empty list if no delegation is happening.

**READ-ONLY:** These values cannot be modified within ABAC rules.

### UserAuthContext Properties

Same properties as `authenticatedUserAuthContext` (see section 3 above).

### Example Rules Using onBehalfOfUserAuthContext

```scala
// 1. Check if onBehalfOf user has active session
onBehalfOfUserAuthContext.exists(ctx => ctx.key == "session_id")

// 2. Verify onBehalfOf user IP is from internal network
onBehalfOfUserAuthContext.exists(ctx =>
  ctx.key == "ip_address" && ctx.value.startsWith("10.0.")
)

// 3. Check if both authenticated and onBehalfOf users have MFA
val authHasMFA = authenticatedUserAuthContext.exists(_.key == "mfa_verified")
val onBehalfHasMFA = onBehalfOfUserAuthContext.exists(_.key == "mfa_verified")
authHasMFA && onBehalfHasMFA

// 4. Verify delegation has auth context
onBehalfOfUserOpt.isDefined && onBehalfOfUserAuthContext.nonEmpty

// 5. Check consumer for delegation
onBehalfOfUserAuthContext.exists(ctx =>
  ctx.consumerId == "trusted-consumer-123"
)
```

---

## 7. user (User)

A user object. This is always available (not optional).

### Available Properties

Same properties as `authenticatedUser` (see section 1 above).

### Example Rules Using user

```scala
// 1. Check user email
user.emailAddress == "alice@example.com"

// 2. Check user by ID
user.userId == "f47ac10b-58cc-4372-a567-0e02b2c3d479"

// 3. Check user provider
user.provider == "obp"

// 4. Compare with authenticated user
user.userId == authenticatedUser.userId

// 5. Check if user owns account (if ownership data available)
accountOpt.exists(account =>
  account.owners.exists(owner => owner.userId == user.userId)
)
```

---

## 8. bankOpt (Option[Bank])

Optional bank context. Present when `bank_id` is provided in the API request.

### Available Properties (when present)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `bankId` | `BankId` | Unique bank identifier | `BankId("gh.29.uk")` |
| `shortName` | `String` | Short name of bank | `"GH Bank"` |
| `fullName` | `String` | Full legal name | `"Great Britain Bank Ltd"` |
| `logoUrl` | `String` | URL to bank logo | `"https://example.com/logo.png"` |
| `websiteUrl` | `String` | Bank website URL | `"https://www.ghbank.co.uk"` |
| `bankRoutingScheme` | `String` | Routing scheme | `"SWIFT_BIC"`, `"UK.SORTCODE"` |
| `bankRoutingAddress` | `String` | Routing address/code | `"GHBKGB2L"` |
| `swiftBic` | `String` | SWIFT BIC code (deprecated) | `"GHBKGB2L"` |
| `nationalIdentifier` | `String` | National identifier (deprecated) | `"123456"` |

### Accessing BankId Value

```scala
// Get the string value from BankId
bankOpt.exists(bank => bank.bankId.value == "gh.29.uk")
```

### Example Rules Using bankOpt

```scala
// 1. Allow only UK banks (by ID prefix)
bankOpt.exists(bank => 
  bank.bankId.value.startsWith("gh.") || 
  bank.bankId.value.startsWith("uk.")
)

// 2. Allow specific bank
bankOpt.exists(bank => bank.bankId.value == "gh.29.uk")

// 3. Check bank name
bankOpt.exists(bank => bank.shortName.contains("GH"))

// 4. Check SWIFT BIC
bankOpt.exists(bank => bank.swiftBic.startsWith("GHBK"))

// 5. Allow if no bank context provided
bankOpt.isEmpty

// 6. Check website URL
bankOpt.exists(bank => bank.websiteUrl.contains(".uk"))
```

---

## 9. accountOpt (Option[BankAccount])

Optional bank account context. Present when `account_id` is provided in the API request.

### Available Properties (when present)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `accountId` | `AccountId` | Unique account identifier | `AccountId("8ca8a7e4-6d02-48e3...")` |
| `accountType` | `String` | Type of account | `"CURRENT"`, `"SAVINGS"`, `"330"` |
| `balance` | `BigDecimal` | Current account balance | `1234.56` |
| `currency` | `String` | Currency code (ISO 4217) | `"GBP"`, `"EUR"`, `"USD"` |
| `name` | `String` | Account name | `"Main Checking Account"` |
| `label` | `String` | Account label | `"Personal Account"` |
| `number` | `String` | Account number | `"12345678"` |
| `bankId` | `BankId` | Bank identifier | `BankId("gh.29.uk")` |
| `lastUpdate` | `Date` | Last transaction refresh date | `Date(...)` |
| `branchId` | `String` | Branch identifier | `"branch-123"` |
| `accountRoutings` | `List[AccountRouting]` | Account routing information | `List(AccountRouting(...))` |
| `accountRules` | `List[AccountRule]` | Account rules (optional) | `List(...)` |
| `accountHolder` | `String` | Account holder name (deprecated) | `"Alice Smith"` |
| `attributes` | `Option[List[Attribute]]` | Account attributes | `Some(List(...))` or `None` |

### Important Notes

- `balance` is a `BigDecimal` - convert to `Double` if needed: `account.balance.toDouble`
- `accountId.value` gives the string value
- `bankId.value` gives the bank ID string
- Use `accountOpt.exists()` to safely check properties

### Example Rules Using accountOpt

```scala
// 1. Check minimum balance
accountOpt.exists(account => account.balance.toDouble >= 1000.0)

// 2. Check account currency
accountOpt.exists(account => account.currency == "GBP")

// 3. Check account type
accountOpt.exists(account => account.accountType == "CURRENT")

// 4. Check account belongs to specific bank
accountOpt.exists(account => account.bankId.value == "gh.29.uk")

// 5. Check account number
accountOpt.exists(account => account.number.startsWith("123"))

// 6. Check if account has label
accountOpt.exists(account => account.label.nonEmpty)

// 7. Complex balance and currency check
accountOpt.exists(account => 
  account.balance.toDouble > 5000.0 && 
  account.currency == "GBP"
)

// 8. Check account attributes (if available)
accountOpt.exists(account => 
  account.attributes.exists(attrs => 
    attrs.exists(attr => attr.name == "accountStatus" && attr.value == "active")
  )
)
```

---

## 10. transactionOpt (Option[Transaction])

Optional transaction context. Present when `transaction_id` is provided in the API request.

Uses the `TransactionCore` type.

### Available Properties (when present)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `id` | `TransactionId` | Unique transaction identifier | `TransactionId("trans-123")` |
| `thisAccount` | `BankAccount` | The account this transaction belongs to | `BankAccount(...)` |
| `otherAccount` | `CounterpartyCore` | The counterparty account | `CounterpartyCore(...)` |
| `transactionType` | `String` | Type of transaction | `"DEBIT"`, `"CREDIT"` |
| `amount` | `BigDecimal` | Transaction amount | `250.00` |
| `currency` | `String` | Currency code | `"GBP"`, `"EUR"`, `"USD"` |
| `description` | `Option[String]` | Transaction description | `Some("Payment to supplier")` or `None` |
| `startDate` | `Date` | Transaction start date | `Date(...)` |
| `finishDate` | `Date` | Transaction completion date | `Date(...)` |
| `balance` | `BigDecimal` | Account balance after transaction | `1234.56` |

### Example Rules Using transactionOpt

```scala
// 1. Allow transactions under a limit
transactionOpt.exists(txn => txn.amount.toDouble < 10000.0)

// 2. Check transaction type
transactionOpt.exists(txn => txn.transactionType == "CREDIT")

// 3. Check transaction currency
transactionOpt.exists(txn => txn.currency == "GBP")

// 4. Check transaction description
transactionOpt.exists(txn => 
  txn.description.exists(desc => desc.contains("salary"))
)

// 5. Check transaction belongs to account
(transactionOpt, accountOpt) match {
  case (Some(txn), Some(account)) =>
    txn.thisAccount.accountId == account.accountId
  case _ => false
}

// 6. Complex amount and type check
transactionOpt.exists(txn => 
  txn.amount.toDouble >= 100.0 && 
  txn.amount.toDouble <= 5000.0 &&
  txn.transactionType == "DEBIT"
)

// 7. Check recent transaction (within 30 days)
import java.time.Instant
import java.time.temporal.ChronoUnit

transactionOpt.exists(txn => {
  val now = Instant.now()
  val txnInstant = txn.finishDate.toInstant
  ChronoUnit.DAYS.between(txnInstant, now) <= 30
})
```

---

## 11. customerOpt (Option[Customer])

Optional customer context. Present when `customer_id` is provided in the API request.

### Available Properties (when present)

| Property | Type | Description | Example |
|----------|------|-------------|---------|
| `customerId` | `String` | Unique customer identifier (UUID) | `"cust-456-789"` |
| `bankId` | `String` | Bank identifier | `"gh.29.uk"` |
| `number` | `String` | Customer number (bank's identifier) | `"CUST123456"` |
| `legalName` | `String` | Legal name of customer | `"Alice Jane Smith"` |
| `mobileNumber` | `String` | Mobile phone number | `"+44 7700 900000"` |
| `email` | `String` | Email address | `"alice@example.com"` |
| `faceImage` | `CustomerFaceImageTrait` | Face image information | `CustomerFaceImage(...)` |
| `dateOfBirth` | `Date` | Date of birth | `Date(1990, 1, 1)` |
| `relationshipStatus` | `String` | Marital status | `"Single"`, `"Married"` |
| `dependents` | `Integer` | Number of dependents | `2` |
| `dobOfDependents` | `List[Date]` | Dates of birth of dependents | `List(Date(...))` |
| `highestEducationAttained` | `String` | Education level | `"Bachelor's Degree"` |
| `employmentStatus` | `String` | Employment status | `"Employed"`, `"Self-Employed"` |
| `creditRating` | `CreditRatingTrait` | Credit rating information | `CreditRating(...)` |
| `creditLimit` | `AmountOfMoneyTrait` | Credit limit | `AmountOfMoney(...)` |
| `kycStatus` | `Boolean` | KYC verification status | `true` or `false` |
| `lastOkDate` | `Date` | Last OK date | `Date(...)` |
| `title` | `String` | Title | `"Mr"`, `"Ms"`, `"Dr"` |
| `branchId` | `String` | Branch identifier | `"branch-123"` |
| `nameSuffix` | `String` | Name suffix | `"Jr"`, `"III"` |

### Example Rules Using customerOpt

```scala
// 1. Check KYC status
customerOpt.exists(customer => customer.kycStatus == true)

// 2. Check customer belongs to bank
customerOpt.exists(customer => customer.bankId == "gh.29.uk")

// 3. Check customer age (over 18)
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

customerOpt.exists(customer => {
  val today = LocalDate.now()
  val birthDate = LocalDate.ofInstant(customer.dateOfBirth.toInstant, ZoneId.systemDefault())
  Period.between(birthDate, today).getYears >= 18
})

// 4. Check employment status
customerOpt.exists(customer => 
  customer.employmentStatus == "Employed" || 
  customer.employmentStatus == "Self-Employed"
)

// 5. Check customer email matches user
customerOpt.exists(customer => customer.email == user.emailAddress)

// 6. Check number of dependents
customerOpt.exists(customer => customer.dependents <= 3)

// 7. Check education level
customerOpt.exists(customer => 
  customer.highestEducationAttained.contains("Degree")
)

// 8. Verify customer and account belong to same bank
(customerOpt, accountOpt) match {
  case (Some(customer), Some(account)) =>
    customer.bankId == account.bankId.value
  case _ => false
}

// 9. Check mobile number is provided
customerOpt.exists(customer => 
  customer.mobileNumber.nonEmpty && customer.mobileNumber != ""
)
```

---

## Complex Rule Examples

### Example 1: Multi-Object Validation

```scala
// Allow if:
// - Authenticated user is admin, OR
// - Authenticated user has finance department attribute, OR
// - User matches authenticated user AND account has sufficient balance

val isAdmin = authenticatedUser.emailAddress.endsWith("@admin.com")
val isFinance = authenticatedUserAttributes.exists(attr => 
  attr.name == "department" && attr.value == "finance"
)
val isSelfAccess = user.userId == authenticatedUser.userId
val hasBalance = accountOpt.exists(_.balance.toDouble > 1000.0)

isAdmin || isFinance || (isSelfAccess && hasBalance)
```

### Example 2: Delegation Check with Attributes

```scala
// Allow if customer service is acting on behalf of someone with proper attributes
val isCustomerService = authenticatedUser.emailAddress.contains("@customerservice.com")
val hasDelegation = onBehalfOfUserOpt.isDefined
val onBehalfHasRole = onBehalfOfUserAttributes.exists(attr =>
  attr.name == "role" && List("customer", "premium_customer").contains(attr.value)
)
val onBehalfHasSession = onBehalfOfUserAuthContext.exists(_.key == "session_id")

isCustomerService && hasDelegation && onBehalfHasRole && onBehalfHasSession
```

### Example 3: Transaction Approval Based on Customer

```scala
// Allow transaction if:
// - Customer is KYC verified AND
// - Transaction is under limit AND
// - Transaction currency matches account

(customerOpt, transactionOpt, accountOpt) match {
  case (Some(customer), Some(txn), Some(account)) =>
    val isKycVerified = customer.kycStatus == true
    val underLimit = txn.amount.toDouble < 10000.0
    val correctCurrency = txn.currency == account.currency
    isKycVerified && underLimit && correctCurrency
  case _ => false
}
```

### Example 4: Bank-Specific Rules

```scala
// Different rules for different banks
bankOpt match {
  case Some(bank) if bank.bankId.value.startsWith("gh.") =>
    // UK bank rules - require higher balance
    accountOpt.exists(_.balance.toDouble > 5000.0)
  case Some(bank) if bank.bankId.value.startsWith("us.") =>
    // US bank rules - require KYC
    customerOpt.exists(_.kycStatus == true)
  case Some(_) =>
    // Other banks - basic check
    user.emailAddress.nonEmpty
  case None =>
    // No bank context - deny
    false
}
```

---

## Working with Optional Objects

All objects except `authenticatedUser`, `authenticatedUserAttributes`, `authenticatedUserAuthContext`, `onBehalfOfUserAttributes`, `onBehalfOfUserAuthContext`, and `user` are optional. Here are patterns for working with them:

### Pattern 1: exists()

```scala
// Check if bank exists and has a property
bankOpt.exists(bank => bank.bankId.value == "gh.29.uk")
```

### Pattern 2: Pattern Matching

```scala
// Match on multiple objects simultaneously
(bankOpt, accountOpt) match {
  case (Some(bank), Some(account)) =>
    bank.bankId == account.bankId
  case _ => false
}
```

### Pattern 3: isDefined / isEmpty

```scala
// Check if object is provided
if (bankOpt.isDefined) {
  val bank = bankOpt.get
  bank.bankId.value == "gh.29.uk"
} else {
  false
}
```

### Pattern 4: for Comprehension

```scala
// Chain multiple optional checks
val result = for {
  bank <- bankOpt
  account <- accountOpt
  if bank.bankId == account.bankId
  if account.balance.toDouble > 1000.0
} yield true

result.getOrElse(false)
```

---

## Common Patterns and Best Practices

### 1. Type Conversions

```scala
// BigDecimal to Double
account.balance.toDouble

// Date comparisons
txn.finishDate.before(new Date())
txn.finishDate.after(new Date())

// String to numeric
account.number.toLong
```

### 2. String Operations

```scala
// Case-insensitive comparison
user.emailAddress.toLowerCase == "alice@example.com"

// Contains check
bank.fullName.contains("Bank")

// Starts with / Ends with
user.emailAddress.endsWith("@admin.com")
bank.bankId.value.startsWith("gh.")
```

### 3. List Operations

```scala
// Check if list is empty
customer.dobOfDependents.isEmpty

// Check list size
customer.dobOfDependents.length > 0

// Find in list
account.accountRoutings.exists(routing => routing.scheme == "IBAN")
```

### 4. Safe Navigation

```scala
// Use getOrElse for defaults
txn.description.getOrElse("No description")

// Chain optional operations
txn.description.getOrElse("No description").toLowerCase.contains("payment")
```

---

## Import Statements Available

These imports are automatically available in your ABAC rule code:

```scala
import com.openbankproject.commons.model._
import code.model.dataAccess.ResourceUser
import net.liftweb.common._
```

You can also use standard Scala/Java imports:

```scala
import java.time._
import java.util.Date
import scala.util._
```

---

## Summary

- **authenticatedUser**: Always available - the logged in user
- **authenticatedUserAttributes**: Always available - list of non-personal attributes for authenticated user (may be empty)
- **authenticatedUserAuthContext**: Always available - list of auth context for authenticated user (may be empty)
- **onBehalfOfUserOpt**: Optional - present when delegation is used
- **onBehalfOfUserAttributes**: Always available - list of non-personal attributes for onBehalfOf user (empty if no delegation)
- **onBehalfOfUserAuthContext**: Always available - list of auth context for onBehalfOf user (empty if no delegation)
- **user**: Always available - a user object
- **bankOpt, accountOpt, transactionOpt, customerOpt**: Optional - use `.exists()` or pattern matching
- **Type conversions**: Remember `.toDouble` for BigDecimal, `.value` for ID types
- **Safe access**: Use `getOrElse()` for Option fields
- **Build incrementally**: Break complex rules into named parts
- **READ-ONLY**: All objects are read-only - you cannot modify them in rules

---

**Last Updated:** 2024  
**Related Documentation:** ABAC_SIMPLE_GUIDE.md, ABAC_REFACTORING.md, ABAC_TESTING_EXAMPLES.md