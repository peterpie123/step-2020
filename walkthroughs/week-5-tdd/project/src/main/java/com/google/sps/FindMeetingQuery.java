// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.google.common.collect.Sets;


/** Finds out times for a possible meeting based on existing meetings. */
public final class FindMeetingQuery {
  /**
   * The overlap of people between a given meeting and the requested meeting. i.e. whether there are
   * required or optional attendees in the existing meeting of question.
   */
  private static enum Overlap {
    NONE, REQUIRED, OPTIONAL
  }

  /** Have end time for meetings be exclusive. */
  private static final boolean TIME_EXCLUSIVE = false;
  /** Have end time for meetings be inclusive. */
  private static final boolean TIME_INCLUSIVE = true;

  /** Construct possible meeting times based on the given events and attendees. */
  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    // Times which fultill requirements (all required attendees)
    List<TimeRange> possibleTimes = new ArrayList<>();
    // Starting out, any time is possible
    possibleTimes.add(TimeRange.WHOLE_DAY);

    Collection<String> requiredAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();

    // Times which work for optional attendees, mapped by name of
    // optional attendee
    Map<String, List<TimeRange>> optionalTimes = new HashMap<>();
    optionalAttendees.forEach(attendee -> {
      // Each attendee can attend the whole day to start
      List<TimeRange> times = new ArrayList<>();
      times.add(TimeRange.WHOLE_DAY);
      optionalTimes.put(attendee, times);
    });

    // Look for collisions with other meetings
    events.stream().forEach(event -> {
      Overlap eventOverlap = isAttendeeOverlap(requiredAttendees, optionalAttendees, event);
      if (eventOverlap == Overlap.REQUIRED) {
        // We can't use this meeting time since at least a required attendee is at another meeting
        subtractTime(possibleTimes, event.getWhen());
      }

      // Handle optional attendees
      getOptional(optionalAttendees, event).forEach(attendee -> {
        subtractTime(optionalTimes.get(attendee), event.getWhen());
      });
    });

    cleanTimes(possibleTimes, request);
    optionalTimes.values().forEach(times -> cleanTimes(times, request));

    // Times that have been reconciled between optional and required attendees
    List<TimeRange> combinedTimes =
        new ArrayList<>(optimizeTimes(possibleTimes, optionalTimes, request));

    cleanTimes(combinedTimes, request);
    return combinedTimes;
  }

  /** Returns the optional attendees at the given event */
  private static Collection<String> getOptional(Collection<String> optionalAttendees, Event event) {
    List<String> out = new ArrayList<>();
    event.getAttendees().stream().filter(attendee -> optionalAttendees.contains(attendee))
        .forEach(attendee -> {
          out.add(attendee);
        });

    return out;
  }

  /** Checks if there is overlap between attendees for the request and the given event. */
  private static Overlap isAttendeeOverlap(Collection<String> requiredAttendees,
      Collection<String> optionalAttendees, Event event) {
    // First check if there are people in this meeting that must be at the requested one, then check
    // for optional attendees
    if (event.getAttendees().stream().anyMatch(e -> requiredAttendees.contains(e))) {
      return Overlap.REQUIRED;
    } else if (event.getAttendees().stream().anyMatch(e -> optionalAttendees.contains(e))) {
      return Overlap.OPTIONAL;
    }
    // No overlap at all!
    return Overlap.NONE;
  }

  /** Remove the time taken up by {@code toSubtract} from available times. */
  private static void subtractTime(Collection<TimeRange> availableTimes, TimeRange toSubtract) {
    // Remove times that are within the range (inclusive) of toSubtract
    availableTimes.removeIf(time -> toSubtract.contains(time));

    // Eke out any times that have the offending time range within them
    List<TimeRange> containTime = availableTimes.stream()
        .filter(time -> time.contains(toSubtract.start()) || time.contains(toSubtract.end()))
        .collect(Collectors.toList());
    // Remove these from available times
    availableTimes.removeAll(containTime);

    // Break up times that include toSubtract
    containTime.forEach(removedTime -> {
      if (removedTime.start() == toSubtract.start()) {
        // Add the time after toSubtract
        availableTimes
            .add(TimeRange.fromStartEnd(toSubtract.end(), removedTime.end(), TIME_EXCLUSIVE));
      } else if (removedTime.end() == toSubtract.end()) {
        // Add the time before toSubtract
        availableTimes
            .add(TimeRange.fromStartEnd(removedTime.start(), toSubtract.start(), TIME_EXCLUSIVE));
      } else {
        // Split the available time in two
        availableTimes
            .add(TimeRange.fromStartEnd(removedTime.start(), toSubtract.start(), TIME_EXCLUSIVE));
        availableTimes
            .add(TimeRange.fromStartEnd(toSubtract.end(), removedTime.end(), TIME_EXCLUSIVE));
      }
    });
  }

  /** Removes times that are too small and sorts times in ascending order. */
  private static void cleanTimes(List<TimeRange> times, MeetingRequest request) {
    // Remove any times that are too small and then sort in ascending order
    times.removeIf(time -> time.duration() < request.getDuration());
    Collections.sort(times, TimeRange.ORDER_BY_START);
  }

  /** Returns any overlap between the given list of times and the time in question. */
  private static List<TimeRange> overlap(Collection<TimeRange> times, TimeRange overlap) {
    List<TimeRange> out = new ArrayList<>();
    times.stream().filter(time -> time.overlaps(overlap)).forEach(time -> {
      int start, end;

      if (time.start() > overlap.start()) {
        start = time.start();
      } else {
        start = overlap.start();
      }
      if (overlap.end() < time.end()) {
        end = overlap.end();
      } else {
        end = time.end();
      }
      out.add(TimeRange.fromStartEnd(start, end, TIME_EXCLUSIVE));
    });
    return out;
  }

  /** Returns the overlap between the two lists of time. */
  private static List<TimeRange> overlap(Collection<TimeRange> times1,
      Collection<TimeRange> times2) {
    List<TimeRange> out = new ArrayList<>();

    times1.forEach(time -> {
      out.addAll(overlap(times2, time));
    });

    return out;
  }

  /**
   * Returns any overlap between required times and optional times
   */
  private static List<TimeRange> reconcileTimes(Collection<TimeRange> requiredTimes,
      Collection<TimeRange> optionalTimes, MeetingRequest request) {
    List<TimeRange> out = new ArrayList<>();

    // Find overlap between times
    out.addAll(overlap(optionalTimes, requiredTimes));
    // Remove times that are too small
    cleanTimes(out, request);

    return out;
  }

  /** Combine the available times of the given attendees */
  private static List<TimeRange> combineTimes(Map<String, List<TimeRange>> times,
      Set<String> attendees) {
    List<TimeRange> out = new ArrayList<>();
    out.add(TimeRange.WHOLE_DAY);

    attendees.forEach(attendee -> {
      List<TimeRange> overlap = overlap(out, times.get(attendee));
      out.clear();
      out.addAll(overlap);
    });
    return out;
  }

  /** Optimize for the most attendees */
  private static Collection<TimeRange> optimizeTimes(Collection<TimeRange> requiredTimes,
      Map<String, List<TimeRange>> optionalTimes, MeetingRequest request) {
    Set<Set<String>> attendeeCombinations = Sets.powerSet(optionalTimes.keySet());

    // First look for largest groups of attendees
    for (int i = optionalTimes.size(); i >= 0; i--) {
      int size = i;
      List<List<TimeRange>> possibleTimes = new ArrayList<>();

      attendeeCombinations.stream().filter(e -> e.size() == size).forEach(attendees -> {
        List<TimeRange> times = combineTimes(optionalTimes, attendees);
        possibleTimes.add(reconcileTimes(requiredTimes, times, request));
      });

      possibleTimes.forEach(times -> {
        cleanTimes(times, request);
      });

      Optional<List<TimeRange>> timeMatch =
          possibleTimes.stream().filter(list -> list.size() > 0).findFirst();
      if (timeMatch.isPresent()) {
        return timeMatch.get();
      }
    }
    if (request.getAttendees().size() > 0) {
      return requiredTimes;
    } else {
      return new ArrayList<>();
    }
  }
}
