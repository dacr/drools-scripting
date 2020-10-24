package fr.janalyse.droolscripting

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.owlike.genson.{Context, Converter}
import com.owlike.genson.stream.{ObjectReader, ObjectWriter}

trait DateFormats {
  val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSS]X")
  val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
}

case class OffsetDateTimeConverter() extends Converter[OffsetDateTime] with DateFormats {
  override def serialize(that: OffsetDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(outputFormatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): OffsetDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else OffsetDateTime.parse(text, inputFormatter)
  }
}

case class ZonedDateTimeConverter() extends Converter[ZonedDateTime] with DateFormats {
  override def serialize(that: ZonedDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(outputFormatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): ZonedDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else ZonedDateTime.parse(text, inputFormatter)
  }
}

case class LocalDateTimeConverter() extends Converter[LocalDateTime] with DateFormats {
  override def serialize(that: LocalDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(outputFormatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): LocalDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else LocalDateTime.parse(text, inputFormatter)
  }
}


case class DateConverter() extends Converter[Date] with DateFormats {
  // TODO - probable performance overhead because of too many conversions
  override def serialize(that: Date, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.toInstant.atOffset(ZoneOffset.UTC).format(outputFormatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): Date = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else {
      val offsetDateTime = OffsetDateTime.parse(text, inputFormatter)
      new Date(offsetDateTime.toInstant.toEpochMilli)
    }
  }
}
