package com.hoangphuc.ddd.application.service.pricing;

import com.hoangphuc.ddd.domain.model.entity.Station;

/**
 * THE PASSENGER'S JOURNEY: from where, to where, how far, and whether that date has a surcharge.
 *
 * Three places need exactly these four values to produce a fare: the trip
 * search screen ("from ... VND"), the seat map (price per seat), and the hold
 * (total amount). Bundled into one type so no place has to look up stations
 * and subtract kilometres on its own — three places computing it themselves
 * are three chances to get three different numbers for the same trip.
 */
public record Journey(Station from, Station to, int distanceKm, double surcharge) {
}
