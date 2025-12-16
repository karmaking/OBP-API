# ABAC Rule Testing Examples

This document provides practical examples for testing ABAC (Attribute-Based Access Control) rules using the refactored ID-based API.

## Prerequisites

1. You need a valid DirectLogin token or other authentication method
2. You must have the `canExecuteAbacRule` entitlement
3. You need to know the IDs of:
   - ABAC rules you want to test
   - Users, banks, accounts, transactions, customers (as needed by your rules)

## API Endpoint

```
POST /obp/v6.0.0/management/abac-rules/{RULE_ID}/execute
```

## Basic Examples

### Example 1: Simple User-Only Rule

Test a rule that only checks user attributes (no bank/account context needed).

**Rule Code:**
```scala
// Rule: Only allow admin users
user.userId.endsWith("@admin.com")
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/admin-only-rule/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{}'
```

**Response:**
```json
{
  "rule_id": "admin-only-rule",
  "rule_name": "Admin Only Access",
  "result": true,
  "message": "Access granted"
}
```

### Example 2: Test Rule for Different User

Test how the rule behaves for a different user (without re-authenticating).

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/admin-only-rule/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "alice@example.com"
  }'
```

**Response:**
```json
{
  "rule_id": "admin-only-rule",
  "rule_name": "Admin Only Access",
  "result": false,
  "message": "Access denied"
}
```

### Example 3: Bank-Specific Rule

Test a rule that checks bank context.

**Rule Code:**
```scala
// Rule: Only allow access to UK banks
bankOpt.exists(bank => 
  bank.bankId.value.startsWith("gh.") || 
  bank.bankId.value.startsWith("uk.")
)
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/uk-banks-only/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bank_id": "gh.29.uk"
  }'
```

**Response:**
```json
{
  "rule_id": "uk-banks-only",
  "rule_name": "UK Banks Only",
  "result": true,
  "message": "Access granted"
}
```

### Example 4: Account Balance Rule

Test a rule that checks account balance.

**Rule Code:**
```scala
// Rule: Only allow if account balance > 1000
accountOpt.exists(account => 
  account.balance.toDouble > 1000.0
)
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/high-balance-only/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
  }'
```

**Response:**
```json
{
  "rule_id": "high-balance-only",
  "rule_name": "High Balance Only",
  "result": true,
  "message": "Access granted"
}
```

### Example 5: Account Ownership Rule

Test a rule that checks if user owns the account.

**Rule Code:**
```scala
// Rule: User must own the account
accountOpt.exists(account => 
  account.owners.exists(owner => owner.userId == user.userId)
)
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/account-owner-only/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "alice@example.com",
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
  }'
```

### Example 6: Transaction Amount Rule

Test a rule that checks transaction amount.

**Rule Code:**
```scala
// Rule: Only allow transactions under 10000
transactionOpt.exists(txn => 
  txn.amount.toDouble < 10000.0
)
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/small-transactions/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
    "transaction_id": "trans-123"
  }'
```

### Example 7: Customer Credit Rating Rule

Test a rule that checks customer credit rating.

**Rule Code:**
```scala
// Rule: Only allow customers with excellent credit
customerOpt.exists(customer => 
  customer.creditRating.getOrElse("") == "EXCELLENT"
)
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/excellent-credit-only/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bank_id": "gh.29.uk",
    "customer_id": "cust-456"
  }'
```

## Complex Examples

### Example 8: Multi-Condition Rule

Test a complex rule with multiple conditions.

**Rule Code:**
```scala
// Rule: Allow if:
// - User is admin, OR
// - User owns account AND balance > 100 AND account is at UK bank
val isAdmin = user.userId.endsWith("@admin.com")
val ownsAccount = accountOpt.exists(_.owners.exists(_.userId == user.userId))
val hasBalance = accountOpt.exists(_.balance.toDouble > 100.0)
val isUKBank = bankOpt.exists(b => 
  b.bankId.value.startsWith("gh.") || b.bankId.value.startsWith("uk.")
)

isAdmin || (ownsAccount && hasBalance && isUKBank)
```

**Test Request (Admin User):**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/complex-access/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "admin@admin.com",
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
  }'
```

**Test Request (Regular User):**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/complex-access/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "alice@example.com",
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0"
  }'
```

### Example 9: Time-Based Rule

Test a rule that includes time-based logic.

**Rule Code:**
```scala
// Rule: Only allow during business hours (9 AM - 5 PM) unless user is admin
import java.time.LocalTime
import java.time.ZoneId

val now = LocalTime.now(ZoneId.of("Europe/London"))
val isBusinessHours = now.isAfter(LocalTime.of(9, 0)) && now.isBefore(LocalTime.of(17, 0))
val isAdmin = user.userId.endsWith("@admin.com")

isAdmin || isBusinessHours
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/business-hours-only/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "user_id": "alice@example.com"
  }'
```

### Example 10: Cross-Entity Validation

Test a rule that validates relationships between entities.

**Rule Code:**
```scala
// Rule: Customer must be associated with the same bank as the account
(customerOpt, accountOpt, bankOpt) match {
  case (Some(customer), Some(account), Some(bank)) =>
    customer.bankId == bank.bankId && 
    account.bankId == bank.bankId
  case _ => false
}
```

**Test Request:**
```bash
curl -X POST \
  'https://api.openbankproject.com/obp/v6.0.0/management/abac-rules/cross-entity-validation/execute' \
  -H 'Authorization: DirectLogin token=eyJhbGciOiJIUzI1...' \
  -H 'Content-Type: application/json' \
  -d '{
    "bank_id": "gh.29.uk",
    "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
    "customer_id": "cust-456"
  }'
```

## Testing Patterns

### Pattern 1: Test Multiple Users

Test the same rule for different users to verify behavior:

```bash
# Test for admin
curl -X POST 'https://.../execute' -d '{"user_id": "admin@admin.com", "bank_id": "gh.29.uk"}'

# Test for regular user
curl -X POST 'https://.../execute' -d '{"user_id": "alice@example.com", "bank_id": "gh.29.uk"}'

# Test for another user
curl -X POST 'https://.../execute' -d '{"user_id": "bob@example.com", "bank_id": "gh.29.uk"}'
```

### Pattern 2: Test Different Banks

Test how the rule behaves across different banks:

```bash
# UK Bank
curl -X POST 'https://.../execute' -d '{"bank_id": "gh.29.uk", "account_id": "acc1"}'

# US Bank
curl -X POST 'https://.../execute' -d '{"bank_id": "us.bank.01", "account_id": "acc2"}'

# German Bank
curl -X POST 'https://.../execute' -d '{"bank_id": "de.bank.01", "account_id": "acc3"}'
```

### Pattern 3: Test Edge Cases

Test boundary conditions:

```bash
# No context (minimal)
curl -X POST 'https://.../execute' -d '{}'

# Partial context
curl -X POST 'https://.../execute' -d '{"bank_id": "gh.29.uk"}'

# Full context
curl -X POST 'https://.../execute' -d '{
  "user_id": "alice@example.com",
  "bank_id": "gh.29.uk",
  "account_id": "8ca8a7e4-6d02-48e3-a029-0b2bf89de9f0",
  "transaction_id": "trans-123",
  "customer_id": "cust-456"
}'

# Invalid IDs (should handle gracefully)
curl -X POST 'https://.../execute' -d '{"bank_id": "invalid-bank-id"}'
```

### Pattern 4: Automated Testing Script

Create a bash script to test multiple scenarios:

```bash
#!/bin/bash

API_BASE="https://api.openbankproject.com/obp/v6.0.0"
TOKEN="eyJhbGciOiJIUzI1..."
RULE_ID="my-test-rule"

test_rule() {
  local description=$1
  local payload=$2
  
  echo "Testing: $description"
  curl -s -X POST \
    "$API_BASE/management/abac-rules/$RULE_ID/execute" \
    -H "Authorization: DirectLogin token=$TOKEN" \
    -H "Content-Type: application/json" \
    -d "$payload" | jq '.result, .message'
  echo "---"
}

# Run tests
test_rule "Admin user" '{"user_id": "admin@admin.com"}'
test_rule "Regular user" '{"user_id": "alice@example.com"}'
test_rule "With bank context" '{"user_id": "alice@example.com", "bank_id": "gh.29.uk"}'
test_rule "With account context" '{"user_id": "alice@example.com", "bank_id": "gh.29.uk", "account_id": "acc1"}'
```

## Error Scenarios

### Error 1: Rule Not Found

```bash
curl -X POST 'https://.../management/abac-rules/nonexistent-rule/execute' \
  -H 'Authorization: DirectLogin token=...' \
  -d '{}'
```

**Response:**
```json
{
  "code": 404,
  "message": "ABAC Rule not found with ID: nonexistent-rule"
}
```

### Error 2: Inactive Rule

If the rule exists but is not active:

**Response:**
```json
{
  "rule_id": "inactive-rule",
  "rule_name": "Inactive Rule",
  "result": false,
  "message": "Execution error: ABAC Rule Inactive Rule is not active"
}
```

### Error 3: Invalid User ID

```bash
curl -X POST 'https://.../execute' \
  -H 'Authorization: DirectLogin token=...' \
  -d '{"user_id": "nonexistent-user"}'
```

**Response:**
```json
{
  "rule_id": "test-rule",
  "rule_name": "Test Rule",
  "result": false,
  "message": "Execution error: User not found"
}
```

### Error 4: Compilation Error

If the rule has invalid Scala code:

**Response:**
```json
{
  "rule_id": "broken-rule",
  "rule_name": "Broken Rule",
  "result": false,
  "message": "Execution error: Failed to compile ABAC rule: ..."
}
```

## Python Testing Example

```python
import requests
import json

class AbacRuleTester:
    def __init__(self, base_url, token):
        self.base_url = base_url
        self.headers = {
            'Authorization': f'DirectLogin token={token}',
            'Content-Type': 'application/json'
        }
    
    def test_rule(self, rule_id, **context):
        """Test an ABAC rule with given context"""
        url = f"{self.base_url}/management/abac-rules/{rule_id}/execute"
        
        # Filter out None values
        payload = {k: v for k, v in context.items() if v is not None}
        
        response = requests.post(url, headers=self.headers, json=payload)
        return response.json()
    
    def test_users(self, rule_id, user_ids, **context):
        """Test rule for multiple users"""
        results = {}
        for user_id in user_ids:
            result = self.test_rule(rule_id, user_id=user_id, **context)
            results[user_id] = result['result']
        return results

# Usage
tester = AbacRuleTester(
    base_url='https://api.openbankproject.com/obp/v6.0.0',
    token='your-token-here'
)

# Test single rule
result = tester.test_rule(
    'admin-only-rule',
    user_id='alice@example.com',
    bank_id='gh.29.uk'
)
print(f"Result: {result['result']}, Message: {result['message']}")

# Test multiple users
users = ['admin@admin.com', 'alice@example.com', 'bob@example.com']
results = tester.test_users('account-owner-rule', users, 
                            bank_id='gh.29.uk',
                            account_id='acc123')
print(results)
# Output: {'admin@admin.com': True, 'alice@example.com': False, ...}
```

## JavaScript Testing Example

```javascript
class AbacRuleTester {
  constructor(baseUrl, token) {
    this.baseUrl = baseUrl;
    this.headers = {
      'Authorization': `DirectLogin token=${token}`,
      'Content-Type': 'application/json'
    };
  }

  async testRule(ruleId, context = {}) {
    const url = `${this.baseUrl}/management/abac-rules/${ruleId}/execute`;
    
    // Remove undefined values
    const payload = Object.fromEntries(
      Object.entries(context).filter(([_, v]) => v !== undefined)
    );

    const response = await fetch(url, {
      method: 'POST',
      headers: this.headers,
      body: JSON.stringify(payload)
    });

    return await response.json();
  }

  async testUsers(ruleId, userIds, context = {}) {
    const results = {};
    for (const userId of userIds) {
      const result = await this.testRule(ruleId, { ...context, user_id: userId });
      results[userId] = result.result;
    }
    return results;
  }
}

// Usage
const tester = new AbacRuleTester(
  'https://api.openbankproject.com/obp/v6.0.0',
  'your-token-here'
);

// Test single rule
const result = await tester.testRule('admin-only-rule', {
  user_id: 'alice@example.com',
  bank_id: 'gh.29.uk'
});
console.log(`Result: ${result.result}, Message: ${result.message}`);

// Test multiple users
const users = ['admin@admin.com', 'alice@example.com', 'bob@example.com'];
const results = await tester.testUsers('account-owner-rule', users, {
  bank_id: 'gh.29.uk',
  account_id: 'acc123'
});
console.log(results);
```

## Best Practices

1. **Start Simple**: Begin with rules that only check user attributes, then add complexity
2. **Test Edge Cases**: Always test with missing IDs, invalid IDs, and partial context
3. **Test Multiple Users**: Verify rule behavior for different user types (admin, owner, guest)
4. **Use Automation**: Create scripts to test multiple scenarios quickly
5. **Document Expected Behavior**: Keep track of what each test should return
6. **Test Both Paths**: Test cases that should allow access AND cases that should deny
7. **Performance Testing**: Test with realistic data volumes to ensure rules perform well

## Troubleshooting

### Rule Always Returns False

- Check if the rule is active (`is_active: true`)
- Verify the rule code compiles successfully
- Ensure all required context IDs are provided
- Check if objects are being fetched successfully

### Rule Times Out

- Rule execution has a 5-second timeout for object fetching
- Simplify rule logic or optimize database queries
- Consider caching frequently accessed objects

### Unexpected Results

- Test with `executeRuleWithObjects` to verify rule logic
- Check object availability (might be `None` if fetch fails)
- Add logging to rule code to debug decision logic
- Verify IDs are correct and objects exist in database

---

**Last Updated:** 2024  
**Related Documentation:** ABAC_REFACTORING.md