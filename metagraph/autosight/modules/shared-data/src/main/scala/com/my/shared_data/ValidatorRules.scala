package com.my.shared_data

import cats.data.Validated
import cats.effect.{Async, Clock}
import cats.implicits.toFunctorOps

import scala.concurrent.duration.DurationInt

import org.tessellation.currency.dataApplication.DataApplicationValidationError
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr

import com.my.shared_data.schema.OnChain

import derevo.circe.magnolia.{decoder, encoder}
import derevo.derive

import scala.util.Try

object ValidatorRules {

  def captureTimeIsCurrent[F[_]: Async](
    captureTime: Long
  ): F[DataApplicationValidationErrorOr[Unit]] =
    Clock[F].realTime.map { now =>
      val currentTime = now.toMillis
      val fiveMinutes = 5.minutes.toMillis
      Validated.condNec(
        (currentTime - fiveMinutes <= captureTime) && 
        (captureTime <= currentTime + fiveMinutes), 
        (), 
        Errors.CaptureTimeNotCurrent
      )
    }

  def latitudeInBounds(value: String): DataApplicationValidationErrorOr[Unit] =
  Validated.condNec(
    Try(value.toDouble).toOption.exists(lat => lat >= -90 && lat <= 90),
    (),
    Errors.LatitudeNotInBounds
  )

  def longitudeInBounds(value: String): DataApplicationValidationErrorOr[Unit] =
  Validated.condNec(
    Try(value.toDouble).toOption.exists(lat => lat >= -180 && lat <= 180),
    (),
    Errors.LongitudeNotInBounds
  )

  def imageRecordDoesNotExist(
    id:    String,
    state: OnChain
  ): DataApplicationValidationErrorOr[Unit] =
    Validated.condNec(!state.images.contains(id), (), Errors.RecordAlreadyExists)

  object Errors {

    @derive(decoder, encoder)
    case object CaptureTimeNotCurrent extends DataApplicationValidationError {
      val message = s"Faild to create image record. Capture time must be within 5 minutes of current time."
    }

    @derive(decoder, encoder)
    case object LatitudeNotInBounds extends DataApplicationValidationError {
      val message = s"Latitude must be a string representing a number between -90 and 90."
    }

    @derive(decoder, encoder)
    case object LongitudeNotInBounds extends DataApplicationValidationError {
      val message = s"Longitude must be string representing a number between -180 and 180."
    }

    @derive(decoder, encoder)
    case object RecordAlreadyExists extends DataApplicationValidationError {
      val message = s"Failed to create image record, previous record found."
    }

    @derive(decoder, encoder)
    case object RewardAddressNotValid extends DataApplicationValidationError {
      val message = s"Provided reward address is not valid."
    }

  }
}
