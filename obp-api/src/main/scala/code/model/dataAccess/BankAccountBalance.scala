package code.model.dataAccess

import com.openbankproject.commons.model._
import net.liftweb.common.Box
import net.liftweb.mapper._
import code.util.Helper

class BankAccountBalance extends BankAccountBalanceTrait with LongKeyedMapper[BankAccountBalance] with CreatedUpdated with IdPK{

  override def getSingleton = BankAccountBalance

  object AccountId_ extends MappedLongForeignKey(this, MappedBankAccount)
  object BalanceType extends MappedString(this, 255)
  //this is the smallest unit of currency! eg. cents, yen, pence, øre, etc.
  object BalanceAmount extends MappedLong(this)

  val foreignMappedBankAccount: Box[MappedBankAccount] = AccountId_.foreign
  val foreignMappedBankAccountCurrency = foreignMappedBankAccount.map(_.currency).getOrElse("EUR")
  
  override def accountId : AccountId = {
    foreignMappedBankAccount.map(_.accountId).getOrElse(AccountId(""))
  }
  override def balanceType: String = BalanceType.get
  override def balanceAmount: BigDecimal = Helper.smallestCurrencyUnitToBigDecimal(BalanceAmount.get, foreignMappedBankAccountCurrency)
  

}

object BankAccountBalance extends BankAccountBalance with LongKeyedMetaMapper[BankAccountBalance] {}
