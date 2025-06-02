package code.bankconnectors.cardano

/*
Open Bank Project - API
Copyright (C) 2011-2017, TESOBE GmbH

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see http://www.gnu.org/licenses/.

Email: contact@tesobe.com
TESOBE GmbH
Osloerstrasse 16/17
Berlin 13359, Germany
*/

import code.api.util.APIUtil._
import code.api.util.CallContext
import code.bankconnectors._
import code.util.AkkaHttpClient._
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model._
import net.liftweb.common._

import java.util.UUID.randomUUID
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.language.postfixOps


trait CardanoConnector_vJun2025 extends Connector with MdcLoggable {
  //this one import is for implicit convert, don't delete

  implicit override val nameOfConnector = CardanoConnector_vJun2025.toString

  val messageFormat: String = "Jun2025"

  override val messageDocs = ArrayBuffer[MessageDoc]()


  override def makePaymentv210(fromAccount: BankAccount,
    toAccount: BankAccount,
    transactionRequestId: TransactionRequestId,
    transactionRequestCommonBody: TransactionRequestCommonBodyJSON,
    amount: BigDecimal,
    description: String,
    transactionRequestType: TransactionRequestType,
    chargePolicy: String,
    callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {

    val transactionData = "123|100.50|EUR|2025-03-16 12:30:00"
    val transactionHash = code.cardano.CardanoMetadataWriter.generateHash(transactionData)

    val txIn = "8c293647e5cb51c4d29e57e162a0bb4a0500096560ce6899a4b801f2b69f2813:0" // This is a  tx_id:0 ///"YOUR_UTXO_HERE"   // Replace with actual UTXO
    val txOut = "addr_test1qruvtthh7mndxu2ncykn47tksar9yqr3u97dlkq2h2dhzwnf3d755n99t92kp4rydpzgv7wmx4nx2j0zzz0g802qvadqtczjhn:1234" // "YOUR_RECEIVER_ADDRESS+LOVELACE" // Replace with receiver address and amount
    val signingKey = "payment.skey" // Path to your signing key file
    val network = "--testnet-magic" // "--testnet-magic 1097911063" // Use --mainnet for mainnet transactions

    code.cardano.CardanoMetadataWriter.submitHashToCardano(transactionHash, txIn, txOut, signingKey, network)
    
    // Simulate a successful transaction ID return
    val transactionId = TransactionId(randomUUID().toString)
    
    Future{(Full(transactionId), callContext)}    
  }

}

object CardanoConnector_vJun2025 extends CardanoConnector_vJun2025
