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
import code.api.util.{CallContext, CustomJsonFormats}
import code.bankconnectors._
import code.util.AkkaHttpClient._
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

//  case class Amount(quantity: Long, unit: String)
//  case class Output(address: String, amount: Amount, assets: List[String])
//  case class Input(address: String, amount: Amount, assets: List[String], id: String, index: Int)
//  case class SlotTime(absolute_slot_number: Long, epoch_number: Int, slot_number: Long, time: String)
//  case class PendingSince(
//    absolute_slot_number: Long,
//    epoch_number: Int,
//    height: Amount,
//    slot_number: Long,
//    time: String
//  )
//  case class ValidityInterval(
//    invalid_before: Amount,
//    invalid_hereafter: Amount
//  )
//  case class TokenContainer(tokens: List[String]) // for mint, burn

  case class TransactionCardano(
//    amount: Amount,
//    burn: TokenContainer,
//    certificates: List[String],
//    collateral: List[String],
//    collateral_outputs: List[String],
//    deposit_returned: Amount,
//    deposit_taken: Amount,
//    direction: String,
//    expires_at: SlotTime,
//    extra_signatures: List[String],
//    fee: Amount,
    id: String//,
//    inputs: List[Input],
//    mint: TokenContainer,
//    outputs: List[Output],
//    pending_since: PendingSince,
//    script_validity: String,
//    status: String,
//    validity_interval: ValidityInterval,
//    withdrawals: List[String]
  )



  override def makePaymentv210(fromAccount: BankAccount,
    toAccount: BankAccount,
    transactionRequestId: TransactionRequestId,
    transactionRequestCommonBody: TransactionRequestCommonBodyJSON,
    amount: BigDecimal,
    description: String,
    transactionRequestType: TransactionRequestType,
    chargePolicy: String,
    callContext: Option[CallContext]): OBPReturnType[Box[TransactionId]] = {
    implicit val formats = CustomJsonFormats.nullTolerateFormats

    case class TransactionCardano2(
      id: String
    )
    
    val paramUrl = "http://localhost:8090/v2/wallets/62b27359c25d4f2a5f97acee521ac1df7ac5a606/transactions"
    val method = "POST"
    val jsonToSend = """{
                       |  "payments": [
                       |    {
                       |      "address": "addr_test1qpv3se9ghq87ud29l0a8asy8nlqwd765e5zt4rc2z4mktqulwagn832cuzcjknfyxwzxz2p2kumx6n58tskugny6mrqs7fd23z",
                       |      "amount": {
                       |        "quantity": 1000000,
                       |        "unit": "lovelace"
                       |      }
                       |    }
                       |  ],
                       |  "passphrase": "StrongPassword123!"
                       |}""".stripMargin
    val request = prepareHttpRequest(paramUrl,  _root_.akka.http.scaladsl.model.HttpMethods.POST, _root_.akka.http.scaladsl.model.HttpProtocol("HTTP/1.1"), jsonToSend) //.withHeaders(buildHeaders(paramUrl,jsonToSend,callContext))
    logger.debug(s"CardanoConnector_vJun2025.makePaymentv210 request is : $request")
    val responseFuture: Future[_root_.akka.http.scaladsl.model.HttpResponse] = makeHttpRequest(request)
    
    val transactionFuture: Future[TransactionCardano2] = responseFuture.flatMap { response =>
      response.entity.dataBytes.runFold(_root_.akka.util.ByteString(""))(_ ++ _).map(_.utf8String).map { jsonString: String =>
        logger.debug(s"CardanoConnector_vJun2025.makePaymentv210 response jsonString is : $jsonString")
        val jValue: JValue = json.parse(jsonString)
        val id = (jValue \ "id").values.toString
        TransactionCardano2(id)
      }
    }

    transactionFuture.map { tx =>
      (Full(TransactionId(tx.id)), callContext)
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

object myApp extends App{

  implicit val formats = CustomJsonFormats.nullTolerateFormats
  
  val aaa ="""{"amount":{"quantity":1168537,"unit":"lovelace"},"burn":{"tokens":[]},"certificates":[],"collateral":[],"collateral_outputs":[],"deposit_returned":{"quantity":0,"unit":"lovelace"},"deposit_taken":{"quantity":0,"unit":"lovelace"},"direction":"outgoing","expires_at":{"absolute_slot_number":97089863,"epoch_number":228,"slot_number":235463,"time":"2025-07-17T17:24:23Z"},"extra_signatures":[],"fee":{"quantity":168537,"unit":"lovelace"},"id":"7aa959f2408ac15b9831a78f6639737c6124610f8b6c9f010bd72f9da2db8aa4","inputs":[{"address":"addr_test1qzq9w0xx8qerljrm59vs02e89vkqxqpwljcra09lr8gmjch9ygp2lexx64xjddk6l3k5k60uelxz3q2dumx99etycq9qnev3j8","amount":{"quantity":4988973201,"unit":"lovelace"},"assets":[],"id":"d8127d7e242d10c0f496fe808989826807806ff5d8ceeb8e3b4c0f31704a6e08","index":1}],"mint":{"tokens":[]},"outputs":[{"address":"addr_test1qpv3se9ghq87ud29l0a8asy8nlqwd765e5zt4rc2z4mktqulwagn832cuzcjknfyxwzxz2p2kumx6n58tskugny6mrqs7fd23z","amount":{"quantity":1000000,"unit":"lovelace"},"assets":[]},{"address":"addr_test1qzw70uzlms3ktewhqly4s45d9m0ks6tueajtjzvhy2y2ft89ygp2lexx64xjddk6l3k5k60uelxz3q2dumx99etycq9q4keup2","amount":{"quantity":4987804664,"unit":"lovelace"},"assets":[]}],"pending_since":{"absolute_slot_number":97082631,"epoch_number":228,"height":{"quantity":3684539,"unit":"block"},"slot_number":228231,"time":"2025-07-17T15:23:51Z"},"script_validity":"valid","status":"pending","validity_interval":{"invalid_before":{"quantity":0,"unit":"slot"},"invalid_hereafter":{"quantity":97089863,"unit":"slot"}},"withdrawals":[]}""".stripMargin

  val aaa1 = """{"id":"123"}"""
  println(aaa)


  case class Amount(quantity: Long, unit: String)
  case class Output(address: String, amount: Amount, assets: List[String])
  case class Input(address: String, amount: Amount, assets: List[String], id: String, index: Int)
  case class SlotTime(absolute_slot_number: Long, epoch_number: Int, slot_number: Long, time: String)
  case class PendingSince(
    absolute_slot_number: Long,
    epoch_number: Int,
    height: Amount,
    slot_number: Long,
    time: String
  )
  case class ValidityInterval(
    invalid_before: Amount,
    invalid_hereafter: Amount
  )
  case class TokenContainer(tokens: List[String]) // for mint, burn

  case class TransactionCardano(
        amount: Amount,
        burn: TokenContainer,
        certificates: List[String],
        collateral: List[String],
        collateral_outputs: List[String],
        deposit_returned: Amount,
        deposit_taken: Amount,
        direction: String,
        expires_at: SlotTime,
        extra_signatures: List[String],
        fee: Amount,
        id: String,
        inputs: List[Input],
        mint: TokenContainer,
        outputs: List[Output],
        pending_since: PendingSince,
        script_validity: String,
        status: String,
        validity_interval: ValidityInterval,
        withdrawals: List[String]
  )
  private val jValue = json.parse(aaa)
  println(jValue)
  private val transaction: TransactionCardano = jValue.extract[TransactionCardano]
  println(transaction)
}
