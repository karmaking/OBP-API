package code.bankaccountbalance

import code.model.dataAccess.MappedBankAccount
import code.util.{Helper, MappedUUID}

import com.openbankproject.commons.model.{AccountId, BalanceId, BankAccountBalanceTrait}
import net.liftweb.common.{Box, Empty, Full, Logger}
import net.liftweb.mapper._


class BankAccountBalance extends BankAccountBalanceTrait with LongKeyedMapper[BankAccountBalance] with CreatedUpdated with IdPK {

  override def getSingleton = BankAccountBalance

  object BalanceId_ extends MappedUUID(this)
//  object AccountId_ extends MappedLongForeignKey(this, MappedBankAccount)
  object AccountId_ extends MappedUUID(this)
  object BalanceType extends MappedString(this, 255)
  //this is the smallest unit of currency! eg. cents, yen, pence, øre, etc.
  object BalanceAmount extends MappedLong(this)

//  val foreignMappedBankAccount: Box[MappedBankAccount] = code.model.dataAccess.MappedBankAccount.find(
//    By(MappedBankAccount.theAccountId, AccountId_.get)
//  )
  val foreignMappedBankAccountCurrency = "EUR" //foreignMappedBankAccount.map(_.currency).getOrElse("EUR")
  
  override def balanceId: BalanceId = BalanceId(BalanceId_.get)
  override def accountId: AccountId = AccountId(AccountId_.get)
  override def balanceType: String = BalanceType.get
  override def balanceAmount: BigDecimal = Helper.smallestCurrencyUnitToBigDecimal(BalanceAmount.get, foreignMappedBankAccountCurrency)
}

object BankAccountBalance extends BankAccountBalance with LongKeyedMetaMapper[BankAccountBalance] {}
