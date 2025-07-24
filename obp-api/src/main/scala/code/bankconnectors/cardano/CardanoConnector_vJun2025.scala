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
import code.api.util.{CallContext, ErrorMessages, NewStyle}
import code.api.v5_1_0.TransactionRequestBodyCardanoJsonV510
import code.bankconnectors._
import code.util.AkkaHttpClient._
import code.util.Helper
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model._
import net.liftweb.common._
import net.liftweb.json
import net.liftweb.json.JValue

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.language.postfixOps


trait CardanoConnector_vJun2025 extends Connector with MdcLoggable {
  //this one import is for implicit convert, don't delete

  implicit override val nameOfConnector = CardanoConnector_vJun2025.toString
  
  val messageFormat: String = "Jun2025"

  override val messageDocs = ArrayBuffer[MessageDoc]()

  override def makePaymentv210(
    fromAccount: BankAccount,
    toAccount: BankAccount,
    transactionRequestId: TransactionRequestId,
    transactionRequestCommonBody: TransactionRequestCommonBodyJSON,
    amount: BigDecimal,
    description: String,
    transactionRequestType: TransactionRequestType,
    chargePolicy: String,
    callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {

    for {
      failMsg <- Future.successful(s"${ErrorMessages.InvalidJsonFormat} The transaction request body should be $TransactionRequestBodyCardanoJsonV510")
      transactionRequestBodyCardano <- NewStyle.function.tryons(failMsg, 400, callContext) {
        transactionRequestCommonBody.asInstanceOf[TransactionRequestBodyCardanoJsonV510]
      }

      walletId = fromAccount.accountId.value
      paramUrl = s"http://localhost:8090/v2/wallets/${walletId}/transactions"
      jsonToSend = s"""{
                     |  "payments": [
                     |    {
                     |      "address": "addr_test1qpv3se9ghq87ud29l0a8asy8nlqwd765e5zt4rc2z4mktqulwagn832cuzcjknfyxwzxz2p2kumx6n58tskugny6mrqs7fd23z",
                     |      "amount": {
                     |        "quantity": ${transactionRequestCommonBody.value.amount},
                     |        "unit": "${transactionRequestCommonBody.value.currency}"
                     |      }
                     |    }
                     |  ],
                     |  "passphrase": "${transactionRequestBodyCardano.passphrase}"
                     |}""".stripMargin

      request = prepareHttpRequest(paramUrl, _root_.akka.http.scaladsl.model.HttpMethods.POST, _root_.akka.http.scaladsl.model.HttpProtocol("HTTP/1.1"), jsonToSend)
      _ = logger.debug(s"CardanoConnector_vJun2025.makePaymentv210 request is : $request")

      response <- NewStyle.function.tryons(s"${ErrorMessages.UnknownError} Failed to make HTTP request to Cardano API", 500, callContext) {
        makeHttpRequest(request)
      }.flatten

      responseBody <- NewStyle.function.tryons(s"${ErrorMessages.UnknownError} Failed to extract response body", 500, callContext) {
        response.entity.dataBytes.runFold(_root_.akka.util.ByteString(""))(_ ++ _).map(_.utf8String)
      }.flatten
      
      _ <- Helper.booleanToFuture(s"${ErrorMessages.UnknownError} Cardano API returned error: ${response.status.value}", 500, callContext) {
        logger.debug(s"CardanoConnector_vJun2025.makePaymentv210 response jsonString is : $responseBody")
        response.status.isSuccess()
      }

      transactionId <- NewStyle.function.tryons(s"${ErrorMessages.InvalidJsonFormat} Failed to parse Cardano API response", 500, callContext) {
        
        val jValue: JValue = json.parse(responseBody)
        val id = (jValue \ "id").values.toString
        if (id.nonEmpty && id != "null") {
          TransactionId(id)
        } else {
          throw new RuntimeException(s"${ErrorMessages.UnknownError} Transaction ID not found in response")
        }
      }

    } yield {
      (Full(transactionId), callContext)
    }
  }
//  override def makePaymentv210(fromAccount: BankAccount,
//    toAccount: BankAccount,
//    transactionRequestId: TransactionRequestId,
//    transactionRequestCommonBody: TransactionRequestCommonBodyJSON,
//    amount: BigDecimal,
//    description: String,
//    transactionRequestType: TransactionRequestType,
//    chargePolicy: String,
//    callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {
//    for {
//      transactionData <- Future.successful("123|100.50|EUR|2025-03-16 12:30:00")
//      transactionHash <- Future {
//        code.cardano.CardanoMetadataWriter.generateHash(transactionData)
//      }
//      txIn <- Future.successful("8c293647e5cb51c4d29e57e162a0bb4a0500096560ce6899a4b801f2b69f2813:0")
//      txOut <- Future.successful("addr_test1qruvtthh7mndxu2ncykn47tksar9yqr3u97dlkq2h2dhzwnf3d755n99t92kp4rydpzgv7wmx4nx2j0zzz0g802qvadqtczjhn:1234")
//      signingKey <- Future.successful("payment.skey")
//      network <- Future.successful("--testnet-magic")
//      _ <- Future {
//        code.cardano.CardanoMetadataWriter.submitHashToCardano(transactionHash, txIn, txOut, signingKey, network)
//      }
//      transactionId <- Future.successful(TransactionId(randomUUID().toString))
//    } yield (Full(transactionId), callContext)
//  }
}

object CardanoConnector_vJun2025 extends CardanoConnector_vJun2025