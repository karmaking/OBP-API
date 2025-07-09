/**
Open Bank Project - API
Copyright (C) 2011-2019, TESOBE GmbH.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

Email: contact@tesobe.com
TESOBE GmbH.
Osloer Strasse 16/17
Berlin 13359, Germany

This product includes software developed at
TESOBE (http://www.tesobe.com/)

  */


package code.model

import code.api.Constant._
import code.api.util.ErrorMessages
import code.metadata.counterparties.Counterparties
import code.views.system.ViewPermission
import com.openbankproject.commons.model._
import com.openbankproject.commons.model.enums.AccountRoutingScheme
import net.liftweb.common._
import net.liftweb.util.StringHelpers

import java.util.Date

case class ViewExtended(val view: View) {

  val viewLogger = Logger(classOf[ViewExtended])

  def getViewPermissions: List[String] =
    if (view.isSystem) {
      ViewPermission.findSystemViewPermissions(view.viewId).map(_.permission.get)
    } else {
      ViewPermission.findCustomViewPermissions(view.bankId, view.accountId, view.viewId).map(_.permission.get)
    }

  def moderateTransaction(transaction : Transaction): Box[ModeratedTransaction] = {
    moderateTransactionUsingModeratedAccount(transaction, moderateAccountLegacy(transaction.thisAccount))
  }

  // In the future we can add a method here to allow someone to show only transactions over a certain limit
  private def moderateTransactionUsingModeratedAccount(transaction: Transaction, moderatedAccount : Option[ModeratedBankAccount]): Box[ModeratedTransaction] = {

    val viewPermissions = getViewPermissions

    lazy val moderatedTransaction = {
      //transaction data
      val transactionId = transaction.id
      val transactionUUID = transaction.uuid
      val otherBankAccount = moderateOtherAccount(transaction.otherAccount)

      //transaction metadata
      val transactionMetadata =
        if(viewPermissions.exists(_ == "canSeeTransactionMetadata"))
        {
          val ownerComment = if (viewPermissions.exists(_ == "canSeeOwnerComment")) Some(transaction.metadata.ownerComment()) else None
          val comments =
            if (viewPermissions.exists(_ == "canSeeComments"))
              Some(transaction.metadata.comments(view.viewId))
            else None
          val addCommentFunc= if(viewPermissions.exists(_ == "canAddComment")) Some(transaction.metadata.addComment) else None
          val deleteCommentFunc =
            if(viewPermissions.exists(_ == "canDeleteComment"))
              Some(transaction.metadata.deleteComment)
            else
              None
          val addOwnerCommentFunc:Option[String=> Boolean] = if (viewPermissions.exists(_ == "canEditOwnerComment")) Some(transaction.metadata.addOwnerComment) else None
          val tags =
            if(viewPermissions.exists(_ == "canSeeTags"))
              Some(transaction.metadata.tags(view.viewId))
            else None
          val addTagFunc =
            if(viewPermissions.exists(_ == "canAddTag"))
              Some(transaction.metadata.addTag)
            else
              None
          val deleteTagFunc =
            if(viewPermissions.exists(_ == "canDeleteTag"))
              Some(transaction.metadata.deleteTag)
            else
              None
          val images =
            if(viewPermissions.exists(_ == "canSeeImages")) Some(transaction.metadata.images(view.viewId))
            else None

          val addImageFunc =
            if(viewPermissions.exists(_ == "canAddImage")) Some(transaction.metadata.addImage)
            else None

          val deleteImageFunc =
            if(viewPermissions.exists(_ == "canDeleteImage")) Some(transaction.metadata.deleteImage)
            else None

          val whereTag =
            if(viewPermissions.exists(_ == "canSeeWhereTag"))
              Some(transaction.metadata.whereTags(view.viewId))
            else
              None

          val addWhereTagFunc : Option[(UserPrimaryKey, ViewId, Date, Double, Double) => Boolean] =
            if(viewPermissions.exists(_ == "canAddWhereTag"))
              Some(transaction.metadata.addWhereTag)
            else
              Empty

          val deleteWhereTagFunc : Option[(ViewId) => Boolean] =
            if (viewPermissions.exists(_ == "canDeleteWhereTag"))
              Some(transaction.metadata.deleteWhereTag)
            else
              Empty


          Some(
            new ModeratedTransactionMetadata(
              ownerComment = ownerComment,
              addOwnerComment = addOwnerCommentFunc,
              comments = comments,
              addComment = addCommentFunc,
              deleteComment = deleteCommentFunc,
              tags = tags,
              addTag = addTagFunc,
              deleteTag = deleteTagFunc,
              images = images,
              addImage = addImageFunc,
              deleteImage = deleteImageFunc,
              whereTag = whereTag,
              addWhereTag = addWhereTagFunc,
              deleteWhereTag = deleteWhereTagFunc
            )
          )
        }
        else
          None

      val transactionType =
        if (viewPermissions.exists(_ == "canSeeTransactionType")) Some(transaction.transactionType)
        else None

      val transactionAmount =
        if (viewPermissions.exists(_ == "canSeeTransactionAmount")) Some(transaction.amount)
        else None

      val transactionCurrency =
        if (viewPermissions.exists(_ == "canSeeTransactionCurrency")) Some(transaction.currency)
        else None

      val transactionDescription =
        if (viewPermissions.exists(_ == "canSeeTransactionDescription")) transaction.description
        else None

      val transactionStartDate =
        if (viewPermissions.exists(_ == "canSeeTransactionStartDate")) Some(transaction.startDate)
        else None

      val transactionFinishDate =
        if (viewPermissions.exists(_ == "canSeeTransactionFinishDate")) Some(transaction.finishDate)
        else None

      val transactionBalance =
        if (viewPermissions.exists(_ == "canSeeTransactionBalance") && transaction.balance != null) transaction.balance.toString()
        else ""

      val transactionStatus =
        if (viewPermissions.exists(_ == "canSeeTransactionStatus")) transaction.status
        else ""

      new ModeratedTransaction(
        UUID = transactionUUID,
        id = transactionId,
        bankAccount = moderatedAccount,
        otherBankAccount = otherBankAccount,
        metadata = transactionMetadata,
        transactionType = transactionType,
        amount = transactionAmount,
        currency = transactionCurrency,
        description = transactionDescription,
        startDate = transactionStartDate,
        finishDate = transactionFinishDate,
        balance = transactionBalance,
        status = transactionStatus
      )
    }


    val belongsToModeratedAccount : Boolean = moderatedAccount match {
      case Some(acc) => acc.accountId == transaction.accountId && acc.bankId == transaction.bankId
      case None => true
    }

    if(!belongsToModeratedAccount) {
      val failMsg = "Attempted to moderate a transaction using the incorrect moderated account"
      view.viewLogger.warn(failMsg)
      Failure(failMsg)
    } else {
      Full(moderatedTransaction)
    }

  }

  private def moderateCore(transactionCore: TransactionCore, moderatedAccount : Option[ModeratedBankAccount]): Box[ModeratedTransactionCore] = {

    val viewPermissions = getViewPermissions

    lazy val moderatedTransaction = {
      //transaction data
      val transactionId = transactionCore.id
      val otherBankAccount = moderateCore(transactionCore.otherAccount)

      val transactionType =
        if (viewPermissions.exists(_ == "canSeeTransactionType")) Some(transactionCore.transactionType)
        else None

      val transactionAmount =
        if (viewPermissions.exists(_ == "canSeeTransactionAmount")) Some(transactionCore.amount)
        else None

      val transactionCurrency =
        if (viewPermissions.exists(_ == "canSeeTransactionCurrency")) Some(transactionCore.currency)
        else None

      val transactionDescription =
        if (viewPermissions.exists(_ == "canSeeTransactionDescription")) transactionCore.description
        else None

      val transactionStartDate =
        if (viewPermissions.exists(_ == "canSeeTransactionStartDate")) Some(transactionCore.startDate)
        else None

      val transactionFinishDate =
        if (viewPermissions.exists(_ == "canSeeTransactionFinishDate")) Some(transactionCore.finishDate)
        else None

      val transactionBalance =
        if (viewPermissions.exists(_ == "canSeeTransactionBalance") && transactionCore.balance != null) transactionCore.balance.toString()
        else ""

      new ModeratedTransactionCore(
        id = transactionId,
        bankAccount = moderatedAccount,
        otherBankAccount = otherBankAccount,
        transactionType = transactionType,
        amount = transactionAmount,
        currency = transactionCurrency,
        description = transactionDescription,
        startDate = transactionStartDate,
        finishDate = transactionFinishDate,
        balance = transactionBalance
      )
    }


    val belongsToModeratedAccount : Boolean = moderatedAccount match {
      case Some(acc) => acc.accountId == transactionCore.thisAccount.accountId && acc.bankId == transactionCore.thisAccount.bankId
      case None => true
    }

    if(!belongsToModeratedAccount) {
      val failMsg = "Attempted to moderate a transaction using the incorrect moderated account"
      view.viewLogger.warn(failMsg)
      Failure(failMsg)
    } else {
      Full(moderatedTransaction)
    }

  }


  def moderateTransactionsWithSameAccount(bank: Bank, transactions : List[Transaction]) : Box[List[ModeratedTransaction]] = {

    val accountUids = transactions.map(t => BankIdAccountId(t.bankId, t.accountId))

    // This function will only accept transactions which have the same This Account.
    if(accountUids.toSet.size > 1) {
      view.viewLogger.warn("Attempted to moderate transactions not belonging to the same account in a call where they should")
      Failure("Could not moderate transactions as they do not all belong to the same account")
    } else {
      Full(transactions.flatMap(
        transaction => {
          // for CBS mode, we can not guarantee this account is the same, each transaction this account fields maybe different, so we need to moderate each transaction using the moderated account.
          val moderatedAccount = moderateAccount(bank, transaction.thisAccount)
          moderateTransactionUsingModeratedAccount(transaction, moderatedAccount)
        })
      )
    }
  }

  def moderateTransactionsWithSameAccountCore(bank: Bank, transactionsCore : List[TransactionCore]) : Box[List[ModeratedTransactionCore]] = {

    val accountUids = transactionsCore.map(t => BankIdAccountId(t.thisAccount.bankId, t.thisAccount.accountId))

    // This function will only accept transactions which have the same This Account.
    if(accountUids.toSet.size > 1) {
      view.viewLogger.warn("Attempted to moderate transactions not belonging to the same account in a call where they should")
      Failure("Could not moderate transactions as they do not all belong to the same account")
    } else {

      Full(transactionsCore.flatMap(
        transaction => {
          // for CBS mode, we can not guarantee this account is the same, each transaction this account fields maybe different, so we need to moderate each transaction using the moderated account.
          val moderatedAccount = moderateAccount(bank, transaction.thisAccount)
          moderateCore(transaction, moderatedAccount)
        })
      )
    }
  }

  /**
   * this is the new function to replace the @moderateAccountLegacy, we will get the bank object from parameter,
   * no need to call the Connector.connector.vend.getBankLegacy several times.
   */
  def moderateAccount(bank: Bank, bankAccount: BankAccount) : Box[ModeratedBankAccount] = {
    val viewPermissions = getViewPermissions

    if(viewPermissions.exists(_ == "canSeeTransactionThisBankAccount"))
    {
      val owners : Set[User] = if(viewPermissions.exists(_ == "canSeeBankAccountOwners")) bankAccount.userOwners else Set()
      val balance = if(viewPermissions.exists(_ == "canSeeBankAccountBalance") && bankAccount.balance != null) bankAccount.balance.toString else ""
      val accountType = if(viewPermissions.exists(_ == "canSeeBankAccountType")) Some(bankAccount.accountType) else None
      val currency = if(viewPermissions.exists(_ == "canSeeBankAccountCurrency")) Some(bankAccount.currency) else None
      val label = if (viewPermissions.exists(_ == "canSeeBankAccountLabel")) Some(bankAccount.label) else None
      val iban = if (viewPermissions.exists(_ == "canSeeBankAccountIban")) bankAccount.accountRoutings.find(_.scheme == AccountRoutingScheme.IBAN.toString).map(_.address) else None
      val number = if (viewPermissions.exists(_ == "canSeeBankAccountNumber")) Some(bankAccount.number) else None
      //From V300, use scheme and address stuff...
      val accountRoutingScheme = if (viewPermissions.exists(_ == "canSeeBankAccountRoutingScheme")) bankAccount.accountRoutings.headOption.map(_.scheme) else None
      val accountRoutingAddress = if (viewPermissions.exists(_ == "canSeeBankAccountRoutingAddress")) bankAccount.accountRoutings.headOption.map(_.address) else None
      val accountRoutings = if (viewPermissions.exists(_ == "canSeeBankAccountRoutingScheme") && viewPermissions.exists(_ == "canSeeBankAccountRoutingAddress")) bankAccount.accountRoutings else Nil
      val accountRules = if (viewPermissions.exists(_ == "canSeeBankAccountCreditLimit")) bankAccount.accountRules else Nil

      //followings are from the bank object.
      val bankId = bank.bankId
      val bankName = if (viewPermissions.exists(_ == "canSeeBankAccountBankName")) Some(bank.fullName) else None
      val nationalIdentifier = if (viewPermissions.exists(_ == "canSeeBankAccountNationalIdentifier")) Some(bank.nationalIdentifier) else None
      val bankRoutingScheme = if (viewPermissions.exists(_ == "canSeeBankRoutingScheme")) Some(bank.bankRoutingScheme) else None
      val bankRoutingAddress = if (viewPermissions.exists(_ == "canSeeBankRoutingAddress")) Some(bank.bankRoutingAddress) else None

      Some(
        new ModeratedBankAccount(
          accountId = bankAccount.accountId,
          owners = Some(owners),
          accountType = accountType,
          balance = balance,
          currency = currency,
          label = label,
          nationalIdentifier = nationalIdentifier,
          iban = iban,
          number = number,
          bankName = bankName,
          bankId = bankId,
          bankRoutingScheme = bankRoutingScheme,
          bankRoutingAddress = bankRoutingAddress,
          accountRoutingScheme = accountRoutingScheme,
          accountRoutingAddress = accountRoutingAddress,
          accountRoutings = accountRoutings,
          accountRules = accountRules
        )
      )
    }
    else
      Failure(s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(CAN_SEE_TRANSACTION_THIS_BANK_ACCOUNT)}` permission on the view(${view.viewId.value})")
  }



  @deprecated("This have the performance issue, call `Connector.connector.vend.getBankLegacy` four times in the backend. use @moderateAccount instead ","08-01-2020")
  def moderateAccountLegacy(bankAccount: BankAccount) : Box[ModeratedBankAccount] = {
    val viewPermissions = getViewPermissions

    if(viewPermissions.exists(_ == "canSeeTransactionThisBankAccount"))
    {
      val owners : Set[User] = if(viewPermissions.exists(_ == "canSeeBankAccountOwners")) bankAccount.userOwners else Set()
      val balance = if(viewPermissions.exists(_ == "canSeeBankAccountBalance") && bankAccount.balance !=null) bankAccount.balance.toString else ""
      val accountType = if(viewPermissions.exists(_ == "canSeeBankAccountType")) Some(bankAccount.accountType) else None
      val currency = if(viewPermissions.exists(_ == "canSeeBankAccountCurrency")) Some(bankAccount.currency) else None
      val label = if(viewPermissions.exists(_ == "canSeeBankAccountLabel")) Some(bankAccount.label) else None
      val nationalIdentifier = if(viewPermissions.exists(_ == "canSeeBankAccountNationalIdentifier")) Some(bankAccount.nationalIdentifier) else None
      val iban = if(viewPermissions.exists(_ == "canSeeBankAccountIban")) bankAccount.accountRoutings.find(_.scheme == AccountRoutingScheme.IBAN.toString).map(_.address) else None
      val number = if(viewPermissions.exists(_ == "canSeeBankAccountNumber")) Some(bankAccount.number) else None
      val bankName = if(viewPermissions.exists(_ == "canSeeBankAccountBankName")) Some(bankAccount.bankName) else None
      val bankId = bankAccount.bankId
      //From V300, use scheme and address stuff...
      val bankRoutingScheme = if(viewPermissions.exists(_ == "canSeeBankRoutingScheme")) Some(bankAccount.bankRoutingScheme) else None
      val bankRoutingAddress = if(viewPermissions.exists(_ == "canSeeBankRoutingAddress")) Some(bankAccount.bankRoutingAddress) else None
      val accountRoutingScheme = if(viewPermissions.exists(_ == "canSeeBankAccountRoutingScheme")) bankAccount.accountRoutings.headOption.map(_.scheme) else None
      val accountRoutingAddress = if(viewPermissions.exists(_ == "canSeeBankAccountRoutingAddress")) bankAccount.accountRoutings.headOption.map(_.address) else None
      val accountRoutings = if(viewPermissions.exists(_ == "canSeeBankAccountRoutingScheme") && viewPermissions.exists(_ == "canSeeBankAccountRoutingAddress")) bankAccount.accountRoutings else Nil
      val accountRules = if(viewPermissions.exists(_ == "canSeeBankAccountCreditLimit")) bankAccount.accountRules else Nil

      Some(
        new ModeratedBankAccount(
          accountId = bankAccount.accountId,
          owners = Some(owners),
          accountType = accountType,
          balance = balance,
          currency = currency,
          label = label,
          nationalIdentifier = nationalIdentifier,
          iban = iban,
          number = number,
          bankName = bankName,
          bankId = bankId,
          bankRoutingScheme = bankRoutingScheme,
          bankRoutingAddress = bankRoutingAddress,
          accountRoutingScheme = accountRoutingScheme,
          accountRoutingAddress = accountRoutingAddress,
          accountRoutings = accountRoutings,
          accountRules = accountRules
        )
      )
    }
    else
      Failure(s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(CAN_SEE_TRANSACTION_THIS_BANK_ACCOUNT)}` permission on the view(${view.viewId.value})")
  }

  def moderateAccountCore(bankAccount: BankAccount) : Box[ModeratedBankAccountCore] = {
    val viewPermissions = getViewPermissions
    
    if(viewPermissions.exists(_ == "canSeeTransactionThisBankAccount"))
    {
      val owners : Set[User] = if(viewPermissions.exists(_ == "canSeeBankAccountOwners")) bankAccount.userOwners else Set()
      val balance = if(viewPermissions.exists(_ == "canSeeBankAccountBalance") && bankAccount.balance != null) Some(bankAccount.balance.toString) else None
      val accountType = if(viewPermissions.exists(_ == "canSeeBankAccountType")) Some(bankAccount.accountType) else None
      val currency = if(viewPermissions.exists(_ == "canSeeBankAccountCurrency")) Some(bankAccount.currency) else None
      val label = if(viewPermissions.exists(_ == "canSeeBankAccountLabel")) Some(bankAccount.label) else None
      val number = if(viewPermissions.exists(_ == "canSeeBankAccountNumber")) Some(bankAccount.number) else None
      val bankId = bankAccount.bankId
      //From V300, use scheme and address stuff...
      val accountRoutings = if(viewPermissions.exists(_ == "canSeeBankAccountRoutingScheme") && viewPermissions.exists(_ == "canSeeBankAccountRoutingAddress")) bankAccount.accountRoutings else Nil
      val accountRules = if(viewPermissions.exists(_ == "canSeeBankAccountCreditLimit")) bankAccount.accountRules else Nil

      Some(
        ModeratedBankAccountCore(
          accountId = bankAccount.accountId,
          owners = Some(owners),
          accountType = accountType,
          balance = balance,
          currency = currency,
          label = label,
          number = number,
          bankId = bankId,
          accountRoutings = accountRoutings,
          accountRules = accountRules
        )
      )
    }
    else
      Failure(s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(CAN_SEE_TRANSACTION_THIS_BANK_ACCOUNT)}` permission on the view(${view.viewId.value})")
  }

  // Moderate the Counterparty side of the Transaction (i.e. the Other Account involved in the transaction)
  def moderateOtherAccount(otherBankAccount : Counterparty) : Box[ModeratedOtherBankAccount] = {
    val viewPermissions = getViewPermissions
    
    if (viewPermissions.exists(_ == "canSeeTransactionOtherBankAccount"))
    {
      //other account data
      val otherAccountId = otherBankAccount.counterpartyId
      val otherAccountLabel: AccountName = {
        val realName = otherBankAccount.counterpartyName

        if (view.usePublicAliasIfOneExists) {

          val publicAlias = Counterparties.counterparties.vend.getPublicAlias(otherBankAccount.counterpartyId).getOrElse("Unknown")

          if (! publicAlias.isEmpty ) AccountName(publicAlias, PublicAlias)
          else AccountName(realName, NoAlias)

        } else if (view.usePrivateAliasIfOneExists) {

          // Note: this assumes that the id in Counterparty and otherBankAccount match!
          val privateAlias = Counterparties.counterparties.vend.getPrivateAlias(otherBankAccount.counterpartyId).getOrElse("Unknown")

          if (! privateAlias.isEmpty) AccountName(privateAlias, PrivateAlias)
          else AccountName(realName, PrivateAlias)
        } else
          AccountName(realName, NoAlias)
      }

      def isAlias = otherAccountLabel.aliasType match {
        case NoAlias => false
        case _ => true
      }

      def moderateField[T](canSeeField: Boolean, field: T) : Option[T] = {
        if(isAlias & view.hideOtherAccountMetadataIfAlias)
          None
        else
          if(canSeeField)
            Some(field)
          else
            None
      }

      implicit def optionStringToString(x : Option[String]) : String = x.getOrElse("")
      val otherAccountNationalIdentifier = if(viewPermissions.exists(_ == "canSeeOtherAccountNationalIdentifier")) Some(otherBankAccount.nationalIdentifier) else None
      val otherAccountSWIFT_BIC = if(viewPermissions.exists(_ == "canSeeOtherAccountSWIFT_BIC")) otherBankAccount.otherBankRoutingAddress else None
      val otherAccountIBAN = if(viewPermissions.exists(_ == "canSeeOtherAccountIBAN")) otherBankAccount.otherAccountRoutingAddress else None
      val otherAccountBankName = if(viewPermissions.exists(_ == "canSeeOtherAccountBankName")) Some(otherBankAccount.thisBankId.value) else None
      val otherAccountNumber = if(viewPermissions.exists(_ == "canSeeOtherAccountNumber")) Some(otherBankAccount.thisAccountId.value) else None
      val otherAccountKind = if(viewPermissions.exists(_ == "canSeeOtherAccountKind")) Some(otherBankAccount.kind) else None
      val otherBankRoutingScheme = if(viewPermissions.exists(_ == "canSeeOtherBankRoutingScheme")) Some(otherBankAccount.otherBankRoutingScheme) else None
      val otherBankRoutingAddress = if(viewPermissions.exists(_ == "canSeeOtherBankRoutingAddress")) otherBankAccount.otherBankRoutingAddress else None
      val otherAccountRoutingScheme = if(viewPermissions.exists(_ == "canSeeOtherAccountRoutingScheme")) Some(otherBankAccount.otherAccountRoutingScheme) else None
      val otherAccountRoutingAddress = if(viewPermissions.exists(_ == "canSeeOtherAccountRoutingAddress")) otherBankAccount.otherAccountRoutingAddress else None
      val otherAccountMetadata =
        if(viewPermissions.exists(_ == "canSeeOtherAccountMetadata")){
          //other bank account metadata
          val moreInfo = moderateField(viewPermissions.exists(_ == "canSeeMoreInfo"), Counterparties.counterparties.vend.getMoreInfo(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val url = moderateField(viewPermissions.exists(_ == "canSeeUrl"), Counterparties.counterparties.vend.getUrl(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val imageUrl = moderateField(viewPermissions.exists(_ == "canSeeImageUrl"), Counterparties.counterparties.vend.getImageURL(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val openCorporatesUrl = moderateField (viewPermissions.exists(_ == "canSeeOpenCorporatesUrl"), Counterparties.counterparties.vend.getOpenCorporatesURL(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val corporateLocation : Option[Option[GeoTag]] = moderateField(viewPermissions.exists(_ == "canSeeCorporateLocation"), Counterparties.counterparties.vend.getCorporateLocation(otherBankAccount.counterpartyId).toOption)
          val physicalLocation : Option[Option[GeoTag]] = moderateField(viewPermissions.exists(_ == "canSeePhysicalLocation"), Counterparties.counterparties.vend.getPhysicalLocation(otherBankAccount.counterpartyId).toOption)
          val addMoreInfo = moderateField(viewPermissions.exists(_ == "canAddMoreInfo"), otherBankAccount.metadata.addMoreInfo)
          val addURL = moderateField(viewPermissions.exists(_ == "canAddURL"), otherBankAccount.metadata.addURL)
          val addImageURL = moderateField(viewPermissions.exists(_ == "canAddImageURL"), otherBankAccount.metadata.addImageURL)
          val addOpenCorporatesUrl = moderateField(viewPermissions.exists(_ == "canAddOpenCorporatesUrl"), otherBankAccount.metadata.addOpenCorporatesURL)
          val addCorporateLocation = moderateField(viewPermissions.exists(_ == "canAddCorporateLocation"), otherBankAccount.metadata.addCorporateLocation)
          val addPhysicalLocation = moderateField(viewPermissions.exists(_ == "canAddPhysicalLocation"), otherBankAccount.metadata.addPhysicalLocation)
          val publicAlias = moderateField(viewPermissions.exists(_ == "canSeePublicAlias"), Counterparties.counterparties.vend.getPublicAlias(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val privateAlias = moderateField(viewPermissions.exists(_ == "canSeePrivateAlias"), Counterparties.counterparties.vend.getPrivateAlias(otherBankAccount.counterpartyId).getOrElse("Unknown"))
          val addPublicAlias = moderateField(viewPermissions.exists(_ == "canAddPublicAlias"), otherBankAccount.metadata.addPublicAlias)
          val addPrivateAlias = moderateField(viewPermissions.exists(_ == "canAddPrivateAlias"), otherBankAccount.metadata.addPrivateAlias)
          val deleteCorporateLocation = moderateField(viewPermissions.exists(_ == "canDeleteCorporateLocation"), otherBankAccount.metadata.deleteCorporateLocation)
          val deletePhysicalLocation= moderateField(viewPermissions.exists(_ == "canDeletePhysicalLocation"), otherBankAccount.metadata.deletePhysicalLocation)

          Some(
            new ModeratedOtherBankAccountMetadata(
              moreInfo = moreInfo,
              url = url,
              imageURL = imageUrl,
              openCorporatesURL = openCorporatesUrl,
              corporateLocation = corporateLocation,
              physicalLocation = physicalLocation,
              publicAlias = publicAlias,
              privateAlias = privateAlias,
              addMoreInfo = addMoreInfo,
              addURL = addURL,
              addImageURL = addImageURL,
              addOpenCorporatesURL = addOpenCorporatesUrl,
              addCorporateLocation = addCorporateLocation,
              addPhysicalLocation = addPhysicalLocation,
              addPublicAlias = addPublicAlias,
              addPrivateAlias = addPrivateAlias,
              deleteCorporateLocation = deleteCorporateLocation,
              deletePhysicalLocation = deletePhysicalLocation
            )
          )
        }
        else
          None

      Some(
        new ModeratedOtherBankAccount(
          id = otherAccountId,
          label = otherAccountLabel,
          nationalIdentifier = otherAccountNationalIdentifier,
          swift_bic = otherAccountSWIFT_BIC,
          iban = otherAccountIBAN,
          bankName = otherAccountBankName,
          number = otherAccountNumber,
          metadata = otherAccountMetadata,
          kind = otherAccountKind,
          bankRoutingScheme = otherBankRoutingScheme ,
          bankRoutingAddress = otherBankRoutingAddress,
          accountRoutingScheme = otherAccountRoutingScheme,
          accountRoutingAddress = otherAccountRoutingAddress
        )
      )
    }
    else
      Failure(s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(CAN_SEE_TRANSACTION_OTHER_BANK_ACCOUNT)}` permission on the view(${view.viewId.value})")
  }

  def moderateCore(counterpartyCore : CounterpartyCore) : Box[ModeratedOtherBankAccountCore] = {
    val viewPermissions = getViewPermissions
    
    if (viewPermissions.exists(_ == "canSeeTransactionOtherBankAccount"))
    {
      //other account data
      val otherAccountId = counterpartyCore.counterpartyId
      val otherAccountLabel: AccountName = AccountName(counterpartyCore.counterpartyName, NoAlias)

      def isAlias = otherAccountLabel.aliasType match {
        case NoAlias => false
        case _ => true
      }

      def moderateField[T](canSeeField: Boolean, field: T) : Option[T] = {
        if(canSeeField)
          Some(field)
        else
          None
      }

      implicit def optionStringToString(x : Option[String]) : String = x.getOrElse("")
      val otherAccountSWIFT_BIC = if(viewPermissions.exists(_ == "canSeeOtherAccountSWIFT_BIC")) counterpartyCore.otherBankRoutingAddress else None
      val otherAccountIBAN = if(viewPermissions.exists(_ == "canSeeOtherAccountIBAN")) counterpartyCore.otherAccountRoutingAddress else None
      val otherAccountBankName = if(viewPermissions.exists(_ == "canSeeOtherAccountBankName")) Some(counterpartyCore.thisBankId.value) else None
      val otherAccountNumber = if(viewPermissions.exists(_ == "canSeeOtherAccountNumber")) Some(counterpartyCore.thisAccountId.value) else None
      val otherAccountKind = if(viewPermissions.exists(_ == "canSeeOtherAccountKind")) Some(counterpartyCore.kind) else None
      val otherBankRoutingScheme = if(viewPermissions.exists(_ == "canSeeOtherBankRoutingScheme")) Some(counterpartyCore.otherBankRoutingScheme) else None
      val otherBankRoutingAddress = if(viewPermissions.exists(_ == "canSeeOtherBankRoutingAddress")) counterpartyCore.otherBankRoutingAddress else None
      val otherAccountRoutingScheme = if(viewPermissions.exists(_ == "canSeeOtherAccountRoutingScheme")) Some(counterpartyCore.otherAccountRoutingScheme) else None
      val otherAccountRoutingAddress = if(viewPermissions.exists(_ == "canSeeOtherAccountRoutingAddress")) counterpartyCore.otherAccountRoutingAddress else None
      Some(
        new ModeratedOtherBankAccountCore(
          id = counterpartyCore.counterpartyId,
          label = otherAccountLabel,
          swift_bic = otherAccountSWIFT_BIC,
          iban = otherAccountIBAN,
          bankName = otherAccountBankName,
          number = otherAccountNumber,
          kind = otherAccountKind,
          bankRoutingAddress = otherBankRoutingScheme,
          bankRoutingScheme = otherBankRoutingAddress,
          accountRoutingScheme = otherAccountRoutingScheme,
          accountRoutingAddress = otherAccountRoutingAddress
        )
      )
    }
    else
      Failure(s"${ErrorMessages.ViewDoesNotPermitAccess} You need the `${StringHelpers.snakify(CAN_SEE_TRANSACTION_OTHER_BANK_ACCOUNT)}` permission on the view(${view.viewId.value})")
  }
}
