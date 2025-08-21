/**
  * Open Bank Project - API
  * Copyright (C) 2011-2019, TESOBE GmbH
  * *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU Affero General Public License for more details.
  * *
  * You should have received a copy of the GNU Affero General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  * *
  * Email: contact@tesobe.com
  * TESOBE GmbH
  * Osloerstrasse 16/17
  * Berlin 13359, Germany
  * *
  * This product includes software developed at
  * TESOBE (http://www.tesobe.com/)
  *
  */
package code.api.v6_0_0

import code.api.util._
import code.util.Helper.MdcLoggable
import com.openbankproject.commons.model._

case class CardanoPaymentJsonV600(
  address: String,
  amount: CardanoAmountJsonV600,
  assets: Option[List[CardanoAssetJsonV600]] = None
)

case class CardanoAmountJsonV600(
  quantity: Long,
  unit: String // "lovelace"
)

case class CardanoAssetJsonV600(
  policy_id: String,
  asset_name: String,
  quantity: Long
)

case class CardanoMetadataStringJsonV600(
  string: String
)

case class TransactionRequestBodyCardanoJsonV600(
  to: CardanoPaymentJsonV600,
  value: AmountOfMoneyJsonV121,
  passphrase: String, 
  description: String,
  metadata: Option[Map[String, CardanoMetadataStringJsonV600]] = None
) extends TransactionRequestCommonBodyJSON

object JSONFactory600 extends CustomJsonFormats with MdcLoggable{
  
}