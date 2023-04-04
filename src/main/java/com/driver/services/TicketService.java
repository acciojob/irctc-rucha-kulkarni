package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    PassengerRepository passengerRepository;


    public Integer bookTicket(BookTicketEntryDto bookTicketEntryDto)throws Exception{
        //Check for validity
        Train train = trainRepository.findById(bookTicketEntryDto.getTrainId()).get();

        List<Ticket> bookedTicketList = train.getBookedTickets();
        int totalSeatBooked = 0;
        //Use bookedTickets List from the TrainRepository to get bookings done against that train
        for(Ticket ticket : bookedTicketList){
            totalSeatBooked += ticket.getPassengersList().size();
        }
        // Incase the there are insufficient tickets
        // throw new Exception("Less tickets are available");
        if(train.getNoOfSeats() < bookTicketEntryDto.getNoOfSeats() + totalSeatBooked){
            throw new Exception("Less tickets are available");
        }

        //Incase the train doesn't pass through the requested stations
        //throw new Exception("Invalid stations");
        String stationList[] = train.getRoute().split(",");
        int startIndex = -1, endIndex = -1 ;
        for(int i=0; i<stationList.length;i++){
            if(stationList[i].equals(bookTicketEntryDto.getFromStation().toString())){
                startIndex=i;
                break;
            }
        }
        for(int i=0; i<stationList.length;i++){
            if(stationList[i].equals(bookTicketEntryDto.getToStation().toString())){
                endIndex=i;
                break;
            }
        }

        if(endIndex - startIndex < 0 || startIndex == -1 || endIndex == -1) {
            throw new Exception("Invalid stations");
        }
        //otherwise book the ticket,
        List<Integer> passengerId = bookTicketEntryDto.getPassengerIds();
        List<Passenger> passengers = new ArrayList<>();

        for(int id : passengerId){
            passengers.add(passengerRepository.findById(id).get());
        }
        //Fare System : Check problem statement
        //calculate the price and other details
        int fair = 0;
        fair = passengers.size()*(endIndex - startIndex)*300;

        //Save the information in corresponding DB Tables
        Ticket ticket= new Ticket();
        ticket.setTrain(train);
        ticket.setFromStation(bookTicketEntryDto.getFromStation());
        ticket.setToStation(bookTicketEntryDto.getToStation());
        ticket.setPassengersList(passengers);
        ticket.setTotalFare(fair);

        //Save the bookedTickets in the train Object
        train.getBookedTickets().add(ticket);
        train.setNoOfSeats(train.getNoOfSeats()-bookTicketEntryDto.getNoOfSeats());

        //Also in the passenger Entity change the attribute bookedTickets by using the attribute bookingPersonId.
        Passenger passenger = passengerRepository.findById(bookTicketEntryDto.getBookingPersonId()).get();
        passenger.getBookedTickets().add(ticket);

        trainRepository.save(train);

        //And the end return the ticketId that has come from db
       return ticketRepository.save(ticket).getTicketId();

    }
}
