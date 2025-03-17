package code.api.berlin.group.v1_3

import code.api.util.APIUtil.DateWithDayFormat
import code.api.util.ErrorMessages.InvalidDateFormat

import java.time.format.{DateTimeFormatter, DateTimeParseException}
import java.time.{LocalDate, ZoneId}
import java.util.Date

object BgSpecValidation {

  val MaxValidDate: LocalDate = LocalDate.parse("9999-12-31")
  val DateFormat: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  def getErrorMessage(dateStr: String): String = {
    validateValidUntil(dateStr) match {
      case Right(validDate) => ""
      case Left(error) => error
    }
  }
  def getDate(dateStr: String): Date = {
    validateValidUntil(dateStr) match {
      case Right(validDate) =>
        Date.from(validDate.atStartOfDay(ZoneId.systemDefault).toInstant)
      case Left(error) =>
        null
    }
  }
  private def validateValidUntil(dateStr: String): Either[String, LocalDate] = {
    try {
      val date = LocalDate.parse(dateStr, DateFormat)
      val today = LocalDate.now()

      if (date.isBefore(today)) {
        Left(s"$InvalidDateFormat Current `validUntil` field is ${dateStr}. The date must not be in the past!")
      } else if (date.isAfter(MaxValidDate)) {
        Left(s"$InvalidDateFormat Current `validUntil` field is ${dateStr}. The maximum allowed date is $MaxValidDate.!")
      } else {
        Right(date) // Valid date
      }
    } catch {
      case e: DateTimeParseException =>
        Left(s"$InvalidDateFormat Current `validUntil` field is ${dateStr}. Please use this format ${DateWithDayFormat.toPattern}!")
    }
  }

  // Example usage
  def main(args: Array[String]): Unit = {
    val testDates = Seq("2025-05-10", "9999-12-31", "2015-01-01", "invalid-date", "2025-01-20T11:04:20Z")

    testDates.foreach { date =>
      validateValidUntil(date) match {
        case Right(validDate) => println(s"Valid date: $validDate")
        case Left(error) => println(s"Error: $error")
      }
    }
  }
}

