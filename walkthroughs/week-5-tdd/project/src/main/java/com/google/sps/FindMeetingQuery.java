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

      // Handle times for optional attendees
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
    // for optional attend