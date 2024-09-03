package com.my.metagraph_l0

import cats.effect.Async
import cats.syntax.all._
import eu.timepit.refined.refineV
import io.circe.syntax._
import io.circe.Json

import org.tessellation.currency.dataApplication.{DataApplicationValidationError, L0NodeContext}
import org.tessellation.json.JsonSerializer
import org.tessellation.node.shared.ext.http4s.SnapshotOrdinalVar
import org.tessellation.schema.address.{Address, DAGAddressRefined}

import com.my.metagraph_l0.ML0NodeContext.syntax._
import com.my.shared_data.lib.{CheckpointService, MetagraphPublicRoutes}
import com.my.shared_data.schema.CalculatedState

import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.circe._

class ML0CustomRoutes[F[_]: Async: JsonSerializer](calculatedStateService: CheckpointService[F, CalculatedState])(
  implicit context: L0NodeContext[F]
) extends MetagraphPublicRoutes[F] {

  protected val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "images" =>
      context.getOnChainState.map(_.map(_.images.toList)).flatMap(prepareResponse(_))

    case GET -> Root / "rewards" =>
      calculatedStateService.get
        .map(_.state.totalRewards.toList.asRight[DataApplicationValidationError])
        .flatMap(prepareResponse(_))

    case GET -> Root / "rewards" / addressStr =>
      refineV[DAGAddressRefined](addressStr) match {
        case Right(dagAddress) =>
          for {
            calculatedState <- calculatedStateService.get
            address = Address(dagAddress)
            rewardAmount = calculatedState.state.totalRewards.getOrElse(address, 0L)
            jsonResponse = Json.obj("rewards" -> rewardAmount.asJson)
            response <- Ok(jsonResponse)
          } yield response
        case Left(error) =>
          BadRequest(s"Invalid address format: $error")
      }

    case GET -> Root / "pending-rewards" =>
      calculatedStateService.get
        .map(_.state.pendingRewards.toList.asRight[DataApplicationValidationError])
        .flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / "latest" =>
      context.getLatestCurrencySnapshot.flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / SnapshotOrdinalVar(ordinal) =>
      context.getCurrencySnapshotAt(ordinal).flatMap(prepareResponse(_))

    case GET -> Root / "snapshot" / "currency" / SnapshotOrdinalVar(ordinal) / "count-updates" =>
      context.countUpdatesInSnapshotAt(ordinal).flatMap(prepareResponse(_))
  }
}
