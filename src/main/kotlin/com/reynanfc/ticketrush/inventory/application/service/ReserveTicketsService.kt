package com.reynanfc.ticketrush.inventory.application.service

import com.reynanfc.ticketrush.inventory.application.port.`in`.command.ReserveTicketsCommand
import com.reynanfc.ticketrush.inventory.application.port.`in`.usecase.ReserveTicketsUseCase
import com.reynanfc.ticketrush.inventory.application.port.out.LotStockRepository
import com.reynanfc.ticketrush.inventory.application.port.out.ReservationRepository
import com.reynanfc.ticketrush.inventory.domain.exception.LotStockNotFoundException
import com.reynanfc.ticketrush.inventory.domain.model.Reservation
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Duration
import java.time.Instant

// Read-modify-write on purpose: under concurrency two requests can read the same `available`
// and both succeed. The concurrency test (#15) exposes it and the lock strategy (#16) fixes it.
class ReserveTicketsService(
    private val lotStockRepository: LotStockRepository,
    private val reservationRepository: ReservationRepository,
    private val clock: Clock,
) : ReserveTicketsUseCase {

    override fun reserve(command: ReserveTicketsCommand): Reservation {
        log.debug(
            "Reserving tickets: orderId={}, lotId={}, quantity={}",
            command.orderId, command.lotId, command.quantity,
        )

        val stock = lotStockRepository.findByLotId(command.lotId)
            ?: throw LotStockNotFoundException(command.lotId)

        val expiresAt = Instant.now(clock).plus(RESERVATION_TTL)
        val reservation = stock.reserve(command.orderId, command.quantity, expiresAt)

        lotStockRepository.save(stock)
        reservationRepository.save(reservation)
        log.info(
            "Tickets reserved: reservationId={}, orderId={}, lotId={}, quantity={}, available={}, expiresAt={}",
            reservation.id, reservation.orderId, reservation.lotId, reservation.quantity, stock.available,
            reservation.expiresAt,
        )

        return reservation
    }

    companion object {
        // How long the tickets stay held while the order is paid (roadmap step 4 expires them).
        val RESERVATION_TTL: Duration = Duration.ofMinutes(10)

        private val log = LoggerFactory.getLogger(ReserveTicketsService::class.java)
    }
}
