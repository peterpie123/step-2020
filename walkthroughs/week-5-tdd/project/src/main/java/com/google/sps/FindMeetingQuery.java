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
import java.util.List;
import java.util.stream.Collectors;

/** Finds out times for a possible meeting based on existing meetings */
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

    // Times which work for optional attendees
    List<TimeRange> optionalTimes = new ArrayList<>();
    // Starting out, any time is possible
    optionalTimes.add(TimeRange.WHOLE_DAY);

    Collection<String> requiredAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();

    // Look for collisions with other meetings
    events.stream().forEach(event -> {
      Overlap eventOverlap = isAttendeeOverlap(requiredAttendees, optionalAttendees, event);
      if (eventOverlap == Overlap.REQUIRED) {
        // We can't use this meeting time since at least a required attendee is at another meeting
        subtractTime(possibleTimes, event.getWhen());
      } else if (eventOverlap == Overlap.OPTIONAL) {
        // This meeting doesn't work for all attendees
        subtractTime(optionalTimes, event.getWhen());
      }
    });

    List<TimeRange> combinedTimes = reconcileTimes(possibleTimes, optionalTimes);

    // Remove any times that are too small and then sort in ascending order
    possibleTimes.removeIf(time -> time.duration() < request.getDuration());
    Collections.sort(possibleTimes, TimeRange.ORDER_BY_START);
    return possibleTimes;
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

  /** Returns any overlap between the given list of times and the time in question */
  private static List<TimeRange> overlap(Collection<TimeRange> times, TimeRange overlap) {
    List<TimeRange> out = new ArrayList<>();
    times.stream().filter(time -> time.overlaps(overlap)).forEach(time -> {
      if (overlap.start() < time.end()) {
        out.add(TimeRange.fromStartEnd(overlap.start(), time.end(), TIME_INCLUSIVE));
      } else {
        out.add(TimeRange.fromStartEnd(time.start(), overlap.end(), TIME_INCLUSIVE));
      }
    });
    return out;
  }

  /**
   * Returns any overlap between required times and optional times, or defers to required only if
   * optional attendees cannot attend
   */
  private static List<TimeRange> reconcileTimes(Collection<TimeRange> requiredTimes,
      Collection<TimeRange> optionalTimes) {
    List<TimeRange> out = new ArrayList<>();
    requiredTimes.forEach(time -> {

    });

    return out;
  }
}
