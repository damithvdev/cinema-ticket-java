package uk.gov.dwp.uc.pairtest;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.Arrays;

public class TicketServiceImpl implements TicketService {

    public static final int MAX_NUMBER_OF_TICKETS = 20;
    public static final int ADULT_TICKET_PRICE = 20;
    public static final int CHILD_TICKET_PRICE = 10;

    private final TicketPaymentService ticketPaymentService;
    private final SeatReservationService seatReservationService;

    public TicketServiceImpl(TicketPaymentService ticketPaymentService, SeatReservationService seatReservationService) {
        this.ticketPaymentService = ticketPaymentService;
        this.seatReservationService = seatReservationService;
    }

    /**
     * Should only have private methods other than the one below.
     */

    @Override
    public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) throws InvalidPurchaseException {

        if (accountId <= 0) {
            throw new InvalidPurchaseException("Invalid account id");
        }
        if (ticketTypeRequests == null || ticketTypeRequests.length == 0) {
            throw new InvalidPurchaseException("Invalid TicketTypeRequest");
        }

        boolean isValidRequest = Arrays.stream(ticketTypeRequests).
                anyMatch(ticketTypeRequest -> ticketTypeRequest.getTicketType() == TicketTypeRequest.Type.ADULT);

        if (!isValidRequest) {
            throw new InvalidPurchaseException("Can not buy tickets without Adult(s)");
        }

        long ticketsCount = Arrays.stream(ticketTypeRequests).mapToInt(TicketTypeRequest::getNoOfTickets).sum();
        if (ticketsCount > MAX_NUMBER_OF_TICKETS) {
            throw new InvalidPurchaseException("Can not buy more than 20 tickets at a time");
        }

        int totalPrice = calculateTotalTicketsPrice(ticketTypeRequests);
        int totalSeats = calculateTotalSeatsToBook(ticketTypeRequests);

        ticketPaymentService.makePayment(accountId, totalPrice);
        seatReservationService.reserveSeat(accountId, totalSeats);
    }

    private int calculateTotalTicketsPrice(TicketTypeRequest[] ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).mapToInt(this::calculateTicketsPriceForType).sum();
    }

    private int calculateTicketsPriceForType(TicketTypeRequest ticketTypeRequest) {
        TicketTypeRequest.Type type = ticketTypeRequest.getTicketType();
        int price = 0;
        if (type == TicketTypeRequest.Type.ADULT) {
            price = ticketTypeRequest.getNoOfTickets() * ADULT_TICKET_PRICE;
        }
        if (type == TicketTypeRequest.Type.CHILD) {
            price = ticketTypeRequest.getNoOfTickets() * CHILD_TICKET_PRICE;
        }
        return price;
    }

    private int calculateTotalSeatsToBook(TicketTypeRequest[] ticketTypeRequests) {
        return Arrays.stream(ticketTypeRequests).mapToInt(this::calculateSeatsForType).sum();
    }

    private int calculateSeatsForType(TicketTypeRequest ticketTypeRequest) {
        TicketTypeRequest.Type type = ticketTypeRequest.getTicketType();
        int noOfSeats = 0;
        if (type == TicketTypeRequest.Type.ADULT || type == TicketTypeRequest.Type.CHILD) {
            //only adults and children have seats
            noOfSeats = ticketTypeRequest.getNoOfTickets();
        }
        return noOfSeats;
    }

}