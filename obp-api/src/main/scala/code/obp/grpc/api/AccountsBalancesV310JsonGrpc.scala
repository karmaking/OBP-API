// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package code.obp.grpc.api

/** getBankAccountsBalances
  */
@SerialVersionUID(0L)
final case class AccountsBalancesV310JsonGrpc(
    accounts: _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc] = _root_.scala.collection.Seq.empty,
    overallBalance: scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = None,
    overallBalanceDate: _root_.scala.Predef.String = ""
    ) extends scalapb.GeneratedMessage with scalapb.Message[AccountsBalancesV310JsonGrpc] with scalapb.lenses.Updatable[AccountsBalancesV310JsonGrpc] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      accounts.foreach(accounts => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(accounts.serializedSize) + accounts.serializedSize)
      if (overallBalance.isDefined) { __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(overallBalance.get.serializedSize) + overallBalance.get.serializedSize }
      if (overallBalanceDate != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, overallBalanceDate) }
      __size
    }
    final override def serializedSize: _root_.scala.Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
      accounts.foreach { __v =>
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      };
      overallBalance.foreach { __v =>
        _output__.writeTag(2, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      };
      {
        val __v = overallBalanceDate
        if (__v != "") {
          _output__.writeString(3, __v)
        }
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): code.obp.grpc.api.AccountsBalancesV310JsonGrpc = {
      val __accounts = (_root_.scala.collection.immutable.Vector.newBuilder[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc] ++= this.accounts)
      var __overallBalance = this.overallBalance
      var __overallBalanceDate = this.overallBalanceDate
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __accounts += _root_.scalapb.LiteParser.readMessage(_input__, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc.defaultInstance)
          case 18 =>
            __overallBalance = Option(_root_.scalapb.LiteParser.readMessage(_input__, __overallBalance.getOrElse(code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc.defaultInstance)))
          case 26 =>
            __overallBalanceDate = _input__.readString()
          case tag => _input__.skipField(tag)
        }
      }
      code.obp.grpc.api.AccountsBalancesV310JsonGrpc(
          accounts = __accounts.result(),
          overallBalance = __overallBalance,
          overallBalanceDate = __overallBalanceDate
      )
    }
    def clearAccounts = copy(accounts = _root_.scala.collection.Seq.empty)
    def addAccounts(__vs: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc*): AccountsBalancesV310JsonGrpc = addAllAccounts(__vs)
    def addAllAccounts(__vs: TraversableOnce[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]): AccountsBalancesV310JsonGrpc = copy(accounts = accounts ++ __vs)
    def withAccounts(__v: _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]): AccountsBalancesV310JsonGrpc = copy(accounts = __v)
    def getOverallBalance: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc = overallBalance.getOrElse(code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc.defaultInstance)
    def clearOverallBalance: AccountsBalancesV310JsonGrpc = copy(overallBalance = None)
    def withOverallBalance(__v: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc): AccountsBalancesV310JsonGrpc = copy(overallBalance = Option(__v))
    def withOverallBalanceDate(__v: _root_.scala.Predef.String): AccountsBalancesV310JsonGrpc = copy(overallBalanceDate = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => accounts
        case 2 => overallBalance.orNull
        case 3 => {
          val __t = overallBalanceDate
          if (__t != "") __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => _root_.scalapb.descriptors.PRepeated(accounts.map(_.toPMessage)(_root_.scala.collection.breakOut))
        case 2 => overallBalance.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 3 => _root_.scalapb.descriptors.PString(overallBalanceDate)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = code.obp.grpc.api.AccountsBalancesV310JsonGrpc
}

object AccountsBalancesV310JsonGrpc extends scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): code.obp.grpc.api.AccountsBalancesV310JsonGrpc = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    code.obp.grpc.api.AccountsBalancesV310JsonGrpc(
      __fieldsMap.getOrElse(__fields.get(0), Nil).asInstanceOf[_root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]],
      __fieldsMap.get(__fields.get(1)).asInstanceOf[scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]],
      __fieldsMap.getOrElse(__fields.get(2), "").asInstanceOf[_root_.scala.Predef.String]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[code.obp.grpc.api.AccountsBalancesV310JsonGrpc] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      code.obp.grpc.api.AccountsBalancesV310JsonGrpc(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]]).getOrElse(_root_.scala.collection.Seq.empty),
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).flatMap(_.as[scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]]),
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = ApiProto.javaDescriptor.getMessageTypes.get(13)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = ApiProto.scalaDescriptor.messages(13)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc
      case 2 => __out = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.scalapb.GeneratedMessageCompanion[_]](
    _root_.code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc,
    _root_.code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc,
    _root_.code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc
  )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = code.obp.grpc.api.AccountsBalancesV310JsonGrpc(
  )
  @SerialVersionUID(0L)
  final case class AmountOfMoneyGrpc(
      currency: _root_.scala.Predef.String = "",
      amount: _root_.scala.Predef.String = ""
      ) extends scalapb.GeneratedMessage with scalapb.Message[AmountOfMoneyGrpc] with scalapb.lenses.Updatable[AmountOfMoneyGrpc] {
      @transient
      private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
      private[this] def __computeSerializedValue(): _root_.scala.Int = {
        var __size = 0
        if (currency != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, currency) }
        if (amount != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, amount) }
        __size
      }
      final override def serializedSize: _root_.scala.Int = {
        var read = __serializedSizeCachedValue
        if (read == 0) {
          read = __computeSerializedValue()
          __serializedSizeCachedValue = read
        }
        read
      }
      def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
        {
          val __v = currency
          if (__v != "") {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = amount
          if (__v != "") {
            _output__.writeString(2, __v)
          }
        };
      }
      def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc = {
        var __currency = this.currency
        var __amount = this.amount
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 => _done__ = true
            case 10 =>
              __currency = _input__.readString()
            case 18 =>
              __amount = _input__.readString()
            case tag => _input__.skipField(tag)
          }
        }
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc(
            currency = __currency,
            amount = __amount
        )
      }
      def withCurrency(__v: _root_.scala.Predef.String): AmountOfMoneyGrpc = copy(currency = __v)
      def withAmount(__v: _root_.scala.Predef.String): AmountOfMoneyGrpc = copy(amount = __v)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = currency
            if (__t != "") __t else null
          }
          case 2 => {
            val __t = amount
            if (__t != "") __t else null
          }
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(currency)
          case 2 => _root_.scalapb.descriptors.PString(amount)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc
  }
  
  object AmountOfMoneyGrpc extends scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = this
    def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc = {
      require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
      val __fields = javaDescriptor.getFields
      code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc(
        __fieldsMap.getOrElse(__fields.get(0), "").asInstanceOf[_root_.scala.Predef.String],
        __fieldsMap.getOrElse(__fields.get(1), "").asInstanceOf[_root_.scala.Predef.String]
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc(
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.javaDescriptor.getNestedTypes.get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc(
    )
    implicit class AmountOfMoneyGrpcLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc](_l) {
      def currency: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.currency)((c_, f_) => c_.copy(currency = f_))
      def amount: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.amount)((c_, f_) => c_.copy(amount = f_))
    }
    final val CURRENCY_FIELD_NUMBER = 1
    final val AMOUNT_FIELD_NUMBER = 2
  }
  
  @SerialVersionUID(0L)
  final case class AccountRoutingGrpc(
      scheme: _root_.scala.Predef.String = "",
      address: _root_.scala.Predef.String = ""
      ) extends scalapb.GeneratedMessage with scalapb.Message[AccountRoutingGrpc] with scalapb.lenses.Updatable[AccountRoutingGrpc] {
      @transient
      private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
      private[this] def __computeSerializedValue(): _root_.scala.Int = {
        var __size = 0
        if (scheme != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, scheme) }
        if (address != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, address) }
        __size
      }
      final override def serializedSize: _root_.scala.Int = {
        var read = __serializedSizeCachedValue
        if (read == 0) {
          read = __computeSerializedValue()
          __serializedSizeCachedValue = read
        }
        read
      }
      def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
        {
          val __v = scheme
          if (__v != "") {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = address
          if (__v != "") {
            _output__.writeString(2, __v)
          }
        };
      }
      def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc = {
        var __scheme = this.scheme
        var __address = this.address
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 => _done__ = true
            case 10 =>
              __scheme = _input__.readString()
            case 18 =>
              __address = _input__.readString()
            case tag => _input__.skipField(tag)
          }
        }
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc(
            scheme = __scheme,
            address = __address
        )
      }
      def withScheme(__v: _root_.scala.Predef.String): AccountRoutingGrpc = copy(scheme = __v)
      def withAddress(__v: _root_.scala.Predef.String): AccountRoutingGrpc = copy(address = __v)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = scheme
            if (__t != "") __t else null
          }
          case 2 => {
            val __t = address
            if (__t != "") __t else null
          }
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(scheme)
          case 2 => _root_.scalapb.descriptors.PString(address)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc
  }
  
  object AccountRoutingGrpc extends scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc] = this
    def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc = {
      require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
      val __fields = javaDescriptor.getFields
      code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc(
        __fieldsMap.getOrElse(__fields.get(0), "").asInstanceOf[_root_.scala.Predef.String],
        __fieldsMap.getOrElse(__fields.get(1), "").asInstanceOf[_root_.scala.Predef.String]
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc(
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.javaDescriptor.getNestedTypes.get(1)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.scalaDescriptor.nestedMessages(1)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc(
    )
    implicit class AccountRoutingGrpcLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc](_l) {
      def scheme: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.scheme)((c_, f_) => c_.copy(scheme = f_))
      def address: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.address)((c_, f_) => c_.copy(address = f_))
    }
    final val SCHEME_FIELD_NUMBER = 1
    final val ADDRESS_FIELD_NUMBER = 2
  }
  
  @SerialVersionUID(0L)
  final case class AccountBalanceV310Grpc(
      id: _root_.scala.Predef.String = "",
      label: _root_.scala.Predef.String = "",
      bankId: _root_.scala.Predef.String = "",
      accountRoutings: _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc] = _root_.scala.collection.Seq.empty,
      balance: scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = None
      ) extends scalapb.GeneratedMessage with scalapb.Message[AccountBalanceV310Grpc] with scalapb.lenses.Updatable[AccountBalanceV310Grpc] {
      @transient
      private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
      private[this] def __computeSerializedValue(): _root_.scala.Int = {
        var __size = 0
        if (id != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, id) }
        if (label != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, label) }
        if (bankId != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(3, bankId) }
        accountRoutings.foreach(accountRoutings => __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(accountRoutings.serializedSize) + accountRoutings.serializedSize)
        if (balance.isDefined) { __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(balance.get.serializedSize) + balance.get.serializedSize }
        __size
      }
      final override def serializedSize: _root_.scala.Int = {
        var read = __serializedSizeCachedValue
        if (read == 0) {
          read = __computeSerializedValue()
          __serializedSizeCachedValue = read
        }
        read
      }
      def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): _root_.scala.Unit = {
        {
          val __v = id
          if (__v != "") {
            _output__.writeString(1, __v)
          }
        };
        {
          val __v = label
          if (__v != "") {
            _output__.writeString(2, __v)
          }
        };
        {
          val __v = bankId
          if (__v != "") {
            _output__.writeString(3, __v)
          }
        };
        accountRoutings.foreach { __v =>
          _output__.writeTag(4, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        };
        balance.foreach { __v =>
          _output__.writeTag(5, 2)
          _output__.writeUInt32NoTag(__v.serializedSize)
          __v.writeTo(_output__)
        };
      }
      def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc = {
        var __id = this.id
        var __label = this.label
        var __bankId = this.bankId
        val __accountRoutings = (_root_.scala.collection.immutable.Vector.newBuilder[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc] ++= this.accountRoutings)
        var __balance = this.balance
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 => _done__ = true
            case 10 =>
              __id = _input__.readString()
            case 18 =>
              __label = _input__.readString()
            case 26 =>
              __bankId = _input__.readString()
            case 34 =>
              __accountRoutings += _root_.scalapb.LiteParser.readMessage(_input__, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc.defaultInstance)
            case 42 =>
              __balance = Option(_root_.scalapb.LiteParser.readMessage(_input__, __balance.getOrElse(code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc.defaultInstance)))
            case tag => _input__.skipField(tag)
          }
        }
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc(
            id = __id,
            label = __label,
            bankId = __bankId,
            accountRoutings = __accountRoutings.result(),
            balance = __balance
        )
      }
      def withId(__v: _root_.scala.Predef.String): AccountBalanceV310Grpc = copy(id = __v)
      def withLabel(__v: _root_.scala.Predef.String): AccountBalanceV310Grpc = copy(label = __v)
      def withBankId(__v: _root_.scala.Predef.String): AccountBalanceV310Grpc = copy(bankId = __v)
      def clearAccountRoutings = copy(accountRoutings = _root_.scala.collection.Seq.empty)
      def addAccountRoutings(__vs: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc*): AccountBalanceV310Grpc = addAllAccountRoutings(__vs)
      def addAllAccountRoutings(__vs: TraversableOnce[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]): AccountBalanceV310Grpc = copy(accountRoutings = accountRoutings ++ __vs)
      def withAccountRoutings(__v: _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]): AccountBalanceV310Grpc = copy(accountRoutings = __v)
      def getBalance: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc = balance.getOrElse(code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc.defaultInstance)
      def clearBalance: AccountBalanceV310Grpc = copy(balance = None)
      def withBalance(__v: code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc): AccountBalanceV310Grpc = copy(balance = Option(__v))
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = id
            if (__t != "") __t else null
          }
          case 2 => {
            val __t = label
            if (__t != "") __t else null
          }
          case 3 => {
            val __t = bankId
            if (__t != "") __t else null
          }
          case 4 => accountRoutings
          case 5 => balance.orNull
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(id)
          case 2 => _root_.scalapb.descriptors.PString(label)
          case 3 => _root_.scalapb.descriptors.PString(bankId)
          case 4 => _root_.scalapb.descriptors.PRepeated(accountRoutings.map(_.toPMessage)(_root_.scala.collection.breakOut))
          case 5 => balance.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc
  }
  
  object AccountBalanceV310Grpc extends scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc] = this
    def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc = {
      require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
      val __fields = javaDescriptor.getFields
      code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc(
        __fieldsMap.getOrElse(__fields.get(0), "").asInstanceOf[_root_.scala.Predef.String],
        __fieldsMap.getOrElse(__fields.get(1), "").asInstanceOf[_root_.scala.Predef.String],
        __fieldsMap.getOrElse(__fields.get(2), "").asInstanceOf[_root_.scala.Predef.String],
        __fieldsMap.getOrElse(__fields.get(3), Nil).asInstanceOf[_root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]],
        __fieldsMap.get(__fields.get(4)).asInstanceOf[scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]]
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
        code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc(
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(3).get).map(_.as[_root_.scala.Predef.String]).getOrElse(""),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(4).get).map(_.as[_root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]]).getOrElse(_root_.scala.collection.Seq.empty),
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(5).get).flatMap(_.as[scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]])
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.javaDescriptor.getNestedTypes.get(2)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.scalaDescriptor.nestedMessages(2)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
      var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
      (__number: @_root_.scala.unchecked) match {
        case 4 => __out = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc
        case 5 => __out = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc
      }
      __out
    }
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc(
    )
    implicit class AccountBalanceV310GrpcLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc](_l) {
      def id: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.id)((c_, f_) => c_.copy(id = f_))
      def label: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.label)((c_, f_) => c_.copy(label = f_))
      def bankId: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.bankId)((c_, f_) => c_.copy(bankId = f_))
      def accountRoutings: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountRoutingGrpc]] = field(_.accountRoutings)((c_, f_) => c_.copy(accountRoutings = f_))
      def balance: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = field(_.getBalance)((c_, f_) => c_.copy(balance = Option(f_)))
      def optionalBalance: _root_.scalapb.lenses.Lens[UpperPB, scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]] = field(_.balance)((c_, f_) => c_.copy(balance = f_))
    }
    final val ID_FIELD_NUMBER = 1
    final val LABEL_FIELD_NUMBER = 2
    final val BANK_ID_FIELD_NUMBER = 3
    final val ACCOUNT_ROUTINGS_FIELD_NUMBER = 4
    final val BALANCE_FIELD_NUMBER = 5
  }
  
  implicit class AccountsBalancesV310JsonGrpcLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc](_l) {
    def accounts: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.collection.Seq[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AccountBalanceV310Grpc]] = field(_.accounts)((c_, f_) => c_.copy(accounts = f_))
    def overallBalance: _root_.scalapb.lenses.Lens[UpperPB, code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc] = field(_.getOverallBalance)((c_, f_) => c_.copy(overallBalance = Option(f_)))
    def optionalOverallBalance: _root_.scalapb.lenses.Lens[UpperPB, scala.Option[code.obp.grpc.api.AccountsBalancesV310JsonGrpc.AmountOfMoneyGrpc]] = field(_.overallBalance)((c_, f_) => c_.copy(overallBalance = f_))
    def overallBalanceDate: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.overallBalanceDate)((c_, f_) => c_.copy(overallBalanceDate = f_))
  }
  final val ACCOUNTS_FIELD_NUMBER = 1
  final val OVERALL_BALANCE_FIELD_NUMBER = 2
  final val OVERALL_BALANCE_DATE_FIELD_NUMBER = 3
}
