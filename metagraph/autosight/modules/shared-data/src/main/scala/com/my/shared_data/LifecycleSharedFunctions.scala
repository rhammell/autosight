package com.my.shared_data

import cats.syntax.all._
import cats.effect.Async

import com.my.shared_data.schema.{OnChain, CalculatedState, ImageRecord, PendingReward}
import com.my.shared_data.schema.Updates.ImageUpdate
import org.tessellation.security.{SecurityProvider, Hasher}
import org.tessellation.security.signature.Signed
import org.tessellation.currency.dataApplication.{DataState, L0NodeContext}
import org.tessellation.ext.cats.syntax.next.catsSyntaxNext
import org.tessellation.schema.epoch.EpochProgress

import com.my.shared_data.Utils.toTokenFormat

object LifecycleSharedFunctions {

  def combine[F[_] : Async](
    inState : DataState[OnChain, CalculatedState],
    updates : List[Signed[ImageUpdate]]
  )(implicit context: L0NodeContext[F], securityProvider: SecurityProvider[F], hasher: Hasher[F]): F[DataState[OnChain, CalculatedState]] = {
    for {
      epochProgress <- context.getLastCurrencySnapshot.flatMap {
        case Some(value) => value.epochProgress.next.pure[F]
        case None =>
          val message = "Could not get the epochProgress from currency snapshot. lastCurrencySnapshot not found"
          new Exception(message).raiseError[F, EpochProgress]
      }

      processedState <- if (updates.isEmpty) {
        processCalculatedState(inState, epochProgress)
      } else {
        updates.foldLeftM(inState) { case (currentState, signedUpdate) =>
          for {
            updatedState <- processUpdate(currentState, signedUpdate, epochProgress)
            finalState <- processCalculatedState(updatedState, epochProgress)
          } yield finalState
        }
      }

    } yield processedState
  }

  private def processCalculatedState[F[_] : Async](
    currentState: DataState[OnChain, CalculatedState],
    epochProgress: EpochProgress
  ): F[DataState[OnChain, CalculatedState]] = {
    val (updatedPendingRewards, removedRewards) = currentState.calculated.pendingRewards.partition(
      _.rewardEpochProgress >= epochProgress
    )
    
    val updatedTotalRewards = removedRewards.foldLeft(currentState.calculated.totalRewards) {
      case (totalRewards, pendingReward) =>
        val currentAmount = totalRewards.getOrElse(pendingReward.rewardAddress, 0L)
        val newAmount = currentAmount + pendingReward.rewardAmount
        totalRewards.updated(pendingReward.rewardAddress, newAmount)
    }
    
    val newCalculated = currentState.calculated.copy(
      totalRewards = updatedTotalRewards,
      pendingRewards = updatedPendingRewards
    )
    
    DataState(currentState.onChain, newCalculated).pure[F]
  }

  private def processUpdate[F[_] : Async : SecurityProvider : Hasher](
    currentState: DataState[OnChain, CalculatedState],
    signedUpdate: Signed[ImageUpdate],
    epochProgress: EpochProgress
  ): F[DataState[OnChain, CalculatedState]] = {
    for {
      deviceAddress <- signedUpdate.proofs.head.id.toAddress[F]
      id <- ImageRecord.generateId[F](signedUpdate.value)
      
      imageRecord = ImageRecord(
        id,
        deviceAddress,
        signedUpdate.value.captureTime,
        signedUpdate.value.imageURL,
        signedUpdate.value.latitude.toDouble,
        signedUpdate.value.longitude.toDouble
      )

      newOnChain = currentState.onChain.copy(
        images = currentState.onChain.images.updated(id, imageRecord)
      )

      newCalculated = signedUpdate.value.rewardAddress match {
        case Some(rewardAddress) =>
          val rewardAmount: Long = 10L
          val newPendingReward = PendingReward(
            rewardAddress = rewardAddress,
            rewardAmount = toTokenFormat(rewardAmount),
            rewardEpochProgress = epochProgress
          )
          currentState.calculated.copy(
            pendingRewards = currentState.calculated.pendingRewards :+ newPendingReward
          )
        case None =>
          currentState.calculated
      }

    } yield DataState(newOnChain, newCalculated)
  }
}