package code.api.util.newstyle

import code.api.util.APIUtil.{OBPReturnType, unboxFullOrFail}
import code.bankconnectors.Connector
import code.api.util.{APIUtil, CallContext}
import code.api.util.CallContext
import code.api.util.ErrorMessages.BankAccountBalanceNotFoundById
import com.openbankproject.commons.ExecutionContext.Implicits.global
import com.openbankproject.commons.model.{AccountId, BankAccountBalanceTrait, BankId}
import com.openbankproject.commons.model.BalanceId


object BankAccountBalanceNewStyle {

  def getBankAccountBalances(
    accountId: AccountId,
    callContext: Option[CallContext]
  ): OBPReturnType[List[BankAccountBalanceTrait]] = {
    Connector.connector.vend.getBankAccountBalancesByAccountId(
      accountId: AccountId,
      callContext: Option[CallContext]
    ) map {
      i => (APIUtil.connectorEmptyResponse(i._1, callContext), i._2)
    }
  }

  def getBankAccountBalanceById(
    balanceId: BalanceId,
    callContext: Option[CallContext]
  ): OBPReturnType[BankAccountBalanceTrait] = {
    Connector.connector.vend.getBankAccountBalanceById(
      balanceId: BalanceId,
      callContext: Option[CallContext]
    ).map {
      result =>
        (
          unboxFullOrFail(
            result._1,
            result._2,
            s"$BankAccountBalanceNotFoundById Current BALANCE_ID(${balanceId.value})",
            404),
          callContext
        )
    }
  }

  def createOrUpdateBankAccountBalance(
    bankId: BankId,
    accountId: AccountId,
    balanceId: Option[BalanceId],
    balanceType: String,
    balanceAmount: BigDecimal,
    callContext: Option[CallContext]
  ): OBPReturnType[BankAccountBalanceTrait] = {
    Connector.connector.vend.createOrUpdateBankAccountBalance(
      bankId: BankId,
      accountId: AccountId,
      balanceId: Option[BalanceId],
      balanceType: String,
      balanceAmount: BigDecimal,
      callContext: Option[CallContext]
    ) map {
      i => (APIUtil.connectorEmptyResponse(i._1, callContext), i._2)
    }
  }

  def deleteBankAccountBalance(
    balanceId: BalanceId,
    callContext: Option[CallContext]
  ): OBPReturnType[Boolean] = {
    Connector.connector.vend.deleteBankAccountBalance(
      balanceId: BalanceId,
      callContext: Option[CallContext]
    ) map {
      i => (APIUtil.connectorEmptyResponse(i._1, callContext), i._2)
    }
  }
  
}