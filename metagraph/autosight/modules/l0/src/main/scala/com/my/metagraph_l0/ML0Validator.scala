package com.my.metagraph_l0

import cats.effect.Async
import cats.implicits.{toFoldableOps, toFunctorOps}

import org.tessellation.currency.dataApplication.dataApplication.DataApplicationValidationErrorOr
import org.tessellation.currency.dataApplication.{DataState}
import org.tessellation.security.Hasher
import org.tessellation.security.signature.Signed

import com.my.shared_data.lib.UpdateValidator
import com.my.shared_data.ValidatorRules
import com.my.shared_data.schema.ImageRecord.generateId
import com.my.shared_data.schema.Updates.{ImageUpdate}
import com.my.shared_data.schema.{CalculatedState, OnChain}

trait ML0Validator[F[_], U, T] extends UpdateValidator[F, U, T]

object ML0Validator {

  type TX = ImageUpdate
  type DS = DataState[OnChain, CalculatedState]

  def make[F[_]: Async: Hasher]: ML0Validator[F, Signed[TX], DS] =
    new ML0Validator[F, Signed[TX], DS] {

      override def verify(state: DS, signedUpdate: Signed[TX]): F[DataApplicationValidationErrorOr[Unit]] =
        validateImageUpdate(signedUpdate.value)(state.onChain)

      private def validateImageUpdate(
        update: ImageUpdate
      ): OnChain => F[DataApplicationValidationErrorOr[Unit]] = (state: OnChain) =>
        for {
          id   <- generateId(update)
          res1 = ValidatorRules.imageRecordDoesNotExist(id, state)
        } yield List(res1).combineAll
    }
}