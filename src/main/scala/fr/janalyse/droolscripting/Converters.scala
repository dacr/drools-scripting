package fr.janalyse.droolscripting

import java.time.{LocalDateTime, OffsetDateTime, ZoneOffset, ZonedDateTime}
import java.time.format.DateTimeFormatter
import java.util.Date

import com.owlike.genson.{Context, Converter}
import com.owlike.genson.stream.{ObjectReader, ObjectWriter}


case class OffsetDateTimeConverter(formatter: DateTimeFormatter) extends Converter[OffsetDateTime]{
  override def serialize(that: OffsetDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(formatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): OffsetDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else OffsetDateTime.parse(text, formatter)
  }
}

case class ZonedDateTimeConverter(formatter: DateTimeFormatter) extends Converter[ZonedDateTime]{
  override def serialize(that: ZonedDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(formatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): ZonedDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else ZonedDateTime.parse(text, formatter)
  }
}

case class LocalDateTimeConverter(formatter: DateTimeFormatter) extends Converter[LocalDateTime]{
  override def serialize(that: LocalDateTime, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.format(formatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): LocalDateTime = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else LocalDateTime.parse(text, formatter)
  }
}


case class DateConverter(formatter: DateTimeFormatter) extends Converter[Date]{
  // TODO - probable performance overhead because of too many conversions
  override def serialize(that: Date, writer: ObjectWriter, ctx: Context): Unit = {
    writer.writeString(that.toInstant.atOffset(ZoneOffset.UTC).format(formatter))
  }
  override def deserialize(reader: ObjectReader, ctx: Context): Date = {
    val text = reader.valueAsString()
    if (text==null || text.isEmpty) null else {
      val offsetDateTime = OffsetDateTime.parse(text, formatter)
      new Date(offsetDateTime.toInstant.toEpochMilli)
    }
  }
}
