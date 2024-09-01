package com.my.data_l1

import cats.effect.Async
import cats.implicits.{toFlatMapOps, toFoldableOps, toFunctorOps}
import cats.syntax.applicative._
import cats.syntax.validated._

import org.tessellation.currency.dataApplication.DataApplicationValidationError
import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.security.Hasher

import com.my.shared_data.lib.UpdateValidator
import com.my.shared_data.ValidatorRules
import com.my.shared_data.schema.ImageRecord.generateId
import com.my.shared_data.schema.Updates.{ImageUpdate}
import com.my.shared_data.schema.{OnChain, Updates}

trait DataL1Validator[F[_], U, T] extends UpdateValidator[F, U, T]

object DataL1Validator {

  def make[F[_]: Async: Hasher]: DataL1Validator[F, ImageUpdate, OnChain] =
    new DataL1Validator[F, ImageUpdate, OnChain] {

      override def verify(state: OnChain, update: ImageUpdate): F[DataApplicationValidationErrorOr[Unit]] =
        validateImageUpdate(update)(state)

      private def validateImageUpdate(
        update: ImageUpdate
      ): OnChain => F[DataApplicationValidationErrorOr[Unit]] = (state: OnChain) =>
        for {
          id   <- generateId(update)
          res1 <- ValidatorRules.captureTimeIsCurrent(update.captureTime)
          res2 = ValidatorRules.latitudeInBounds(update.latitude)
          res3 = ValidatorRules.longitudeInBounds(update.longitude)
        } yield List(res1, res2, res3).combineAll
    }
}