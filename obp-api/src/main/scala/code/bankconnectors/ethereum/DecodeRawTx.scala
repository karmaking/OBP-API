package code.bankconnectors.ethereum

import java.math.BigInteger
import org.web3j.crypto.{Hash, RawTransaction, TransactionDecoder, Sign, SignedRawTransaction}
import org.web3j.utils.{Numeric => W3Numeric}
import net.liftweb.json._

object DecodeRawTx {

  private def fatal(msg: String): Nothing = {
    Console.err.println(msg)
    sys.exit(1)
  }

  // File/stdin helpers removed; input is provided as a function parameter now.

  private def normalizeHex(hex: String): String = {
    val h = Option(hex).getOrElse("").trim
    if (!h.startsWith("0x")) fatal("Input must start with 0x")
    val body = h.drop(2)
    if (!body.matches("[0-9a-fA-F]+")) fatal("Invalid hex characters in input")
    if (body.length % 2 != 0) fatal("Hex string length must be even")
    "0x" + body.toLowerCase
  }

  private def detectType(hex: String): Int = {
    val body = hex.stripPrefix("0x")
    if (body.startsWith("02")) 2
    else if (body.startsWith("01")) 1
    else 0
  }

  // Build EIP-155 v when chainId is available; parity from v-byte (27/28 -> 0/1, otherwise lowest bit)
  private def vToHex(sig: Sign.SignatureData, chainIdOpt: Option[BigInteger]): String = {
    val vb: Int = {
      val arr = sig.getV
      if (arr != null && arr.length > 0) java.lang.Byte.toUnsignedInt(arr(0)) else 0
    }
    chainIdOpt match {
      case Some(cid) if cid.signum() > 0 =>
        val parity = if (vb == 27 || vb == 28) vb - 27 else (vb & 1)
        val v = cid.multiply(BigInteger.valueOf(2)).add(BigInteger.valueOf(35L + parity))
        "0x" + v.toString(16)
      case _ =>
        "0x" + Integer.toHexString(vb)
    }
  }

  private def jStrOrNull(v: String): JValue = if (v == null) JNull else JString(v)
  private def jOptStrOrNull(v: Option[String]): JValue = v.map(JString).getOrElse(JNull)

  /**
    * Decode raw Ethereum transaction hex and return a JSON string summarizing the fields.
    * The input must be a 0x-prefixed hex string; no file or stdin reading is performed.
    */
  def decodeRawTxToJson(rawIn: String): String = {
    val rawHex  = normalizeHex(rawIn)
    val txType  = detectType(rawHex)

    val decoded: RawTransaction =
      try TransactionDecoder.decode(rawHex)
      catch { case e: Throwable => fatal(s"decode failed: ${e.getMessage}") }

    val (fromOpt, chainIdOpt, vHexOpt, rHexOpt, sHexOpt):
      (Option[String], Option[BigInteger], Option[String], Option[String], Option[String]) = decoded match {
      case srt: SignedRawTransaction =>
        val from = Option(srt.getFrom)
        val cid: Option[BigInteger] = try {
          val c = srt.getChainId // long in web3j 4.x; -1 if absent
          if (c > 0L) Some(BigInteger.valueOf(c)) else None
        } catch { case _: Throwable => None }
        val sig  = srt.getSignatureData
        val vH   = vToHex(sig, cid)
    val rH   = W3Numeric.toHexString(sig.getR)
    val sH   = W3Numeric.toHexString(sig.getS)
        (from, cid, Some(vH), Some(rH), Some(sH))
      case _ =>
        (None, None, None, None, None)
    }

    val hash        = Hash.sha3(rawHex)
    val gasPriceHex = Option(decoded.getGasPrice).map(W3Numeric.toHexStringWithPrefix).getOrElse(null)
    val gasLimitHex = Option(decoded.getGasLimit).map(W3Numeric.toHexStringWithPrefix).getOrElse(null)
    val valueHex    = Option(decoded.getValue).map(W3Numeric.toHexStringWithPrefix).getOrElse(null)
    val nonceHex    = Option(decoded.getNonce).map(W3Numeric.toHexStringWithPrefix).getOrElse(null)
    val toAddr      = decoded.getTo
    val inputData   = Option(decoded.getData).getOrElse("0x")

    val estimatedFeeHex =
      (for {
        gp <- Option(decoded.getGasPrice)
        gl <- Option(decoded.getGasLimit)
      } yield W3Numeric.toHexStringWithPrefix(gp.multiply(gl))).getOrElse(null)

    val j = JObject(List(
      JField("hash", JString(hash)),
      JField("type", JString(txType.toString)),
      JField("chainId", chainIdOpt.map(cid => JString(cid.toString)).getOrElse(JNull)),
      JField("nonce", jStrOrNull(nonceHex)),
      JField("gasPrice", jStrOrNull(gasPriceHex)),
      JField("gas", jStrOrNull(gasLimitHex)),
      JField("to", jStrOrNull(toAddr)),
      JField("value", jStrOrNull(valueHex)),
      JField("input", jStrOrNull(inputData)),
      JField("from", jOptStrOrNull(fromOpt)),
      JField("v", jOptStrOrNull(vHexOpt)),
      JField("r", jOptStrOrNull(rHexOpt)),
      JField("s", jOptStrOrNull(sHexOpt)),
      JField("estimatedFee", jStrOrNull(estimatedFeeHex))
    ))
    compactRender(j)
  }

  def main(args: Array[String]): Unit = {
    val raxHex = "0xf86b178203e882520894627306090abab3a6e1400e9345bc60c78a8bef57880de0b6b3a764000080820ff6a016878a008fb817df6d771749336fa0c905ec5b7fafcd043f0d9e609a2b5e41e0a0611dbe0f2ee2428360c72f4287a2996cb0d45cb8995cc23eb6ba525cb9580e02"
    val out = decodeRawTxToJson(raxHex)
    print(out)
  }
}