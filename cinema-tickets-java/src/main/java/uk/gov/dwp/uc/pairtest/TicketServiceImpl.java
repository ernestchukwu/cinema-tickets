package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {
    /**
     * Should only have private methods other than the one below.
     */
    private static final int MAX_NUMBER_OF_TICKETS = 25;

    private static final int ADULT_TICKET_PRICE = 25;
    private static final int CHILD_TICKET_PRICE = 15;
    private static final int INFANT_TICKET_PRICE = 0;

    private static final char CURRENCY_SYMBOL = '£';

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    private void validateAccountId(Long accountId) {
        if (accountId == null || accountId <= 0L) {
            throw new InvalidPurchaseException("Account with ID [" + accountId + "] is invalid.");
        }
    }

    private void validateTicketPurchaseRequest(TicketTypeRequest... ticketTypeRequests) {

        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Ticket requests must be provided and contain at least one request.");
        }

        if (Arrays.stream(ticketTypeRequests).anyMatch(
                (request) -> request.getNoOfTickets() < 0)) {
            throw new InvalidPurchaseException("Number of tickets cannot be negative.");
        }

        if (Arrays.stream(ticketTypeRequests).anyMatch(
                (request) -> request.getTicketType() == null)) {
            throw new InvalidPurchaseException("Ticket type cannot be null.");
        }

        if (Arrays.stream(ticketTypeRequests).mapToInt(
                TicketTypeRequest::getNoOfTickets).sum() > MAX_NUMBER_OF_TICKETS) {
            throw new InvalidPurchaseException("Only a maximum of " + MAX_NUMBER_OF_TICKETS + " tickets can be purchased at a time.");
        }

        int noOfAdultTickets = countTickets(ticketTypeRequests, TicketTypeRequest.Type.ADULT);
        int noOfChildTickets = countTickets(ticketTypeRequests, TicketTypeRequest.Type.CHILD);
        int noOfInfantTickets = countTickets(ticketTypeRequests, TicketTypeRequest.Type.INFANT);

        if ((noOfChildTickets > 0 || noOfInfantTickets > 0) && noOfAdultTickets == 0) {
            throw new InvalidPurchaseException(
                    "Requests containing CHILD or INFANT tickets must contain at least one ADULT ticket.");
        }
    }

    private int countTickets(TicketTypeRequest[] ticketRequests, TicketTypeRequest.Type type) {
        return Arrays.stream(ticketRequests).filter(
                (request) -> request.getTicketType() == type)
                .mapToInt(TicketTypeRequest::getNoOfTickets).sum();
    }

    private int computeTotalNumberOfSeats(TicketTypeRequest... ticketRequests) {
        return Arrays.stream(ticketRequests).filter(
                (request) -> request.getTicketType() != TicketTypeRequest.Type.INFANT)
                .mapToInt(TicketTypeRequest::getNoOfTickets).sum();
    }

    private int computeTotalAmountToBePaid(TicketTypeRequest... ticketRequests) {
        return Arrays.stream(ticketRequests).mapToInt(
                (request) -> getTicketPrice(request.getTicketType()) * request.getNoOfTickets()).sum();
    }

    private int getTicketPrice(TicketTypeRequest.Type type) {
        switch (type) {
            case ADULT:
                return ADULT_TICKET_PRICE;
            case CHILD:
                return CHILD_TICKET_PRICE;
            case INFANT:
                return INFANT_TICKET_PRICE;
            default:
                throw new IllegalArgumentException("Unexpected ticket type [" + type + "].");
        }
    }

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        validateAccountId(accountId);
        validateTicketPurchaseRequest(ticketTypeRequests);

        int totalSeats = computeTotalNumberOfSeats(ticketTypeRequests);
        int totalAmount = computeTotalAmountToBePaid(ticketTypeRequests);

        seatReservationService.reserveSeat(accountId, totalSeats);
        ticketPaymentService.makePayment(accountId, totalAmount);

        System.out.println("Tickets purchased [Account ID: " + accountId + ", Number of Seats: " + totalSeats +
                ", Total amount: " + CURRENCY_SYMBOL + totalAmount + "].");
    }

}
