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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class FindMeetingQuery {
  /** The type of overlap between a given meeting and the requested meeting */
  private static enum OverlapType {
    NONE, REQUIRED, OPTIONAL
  }

  public Collection<TimeRange> query(Collection<Event> events, MeetingRequest request) {
    Set<TimeRange> possibleTimes = new HashSet<>();
    // Starting out, any time is possible
    possibleTimes.add(TimeRange.WHOLE_DAY);

    Collection<String> requiredAttendees = request.getAttendees();
    Collection<String> optionalAttendees = request.getOptionalAttendees();

    // Look for collisions with other meetings
    events.stream().forEach(event -> {
      if (isAttendeeOverlap(requiredAttendees, optionalAttendees, event) == OverlapType.REQUIRED) {
        // We can't use this meeting time
        subtractTime(possibleTimes, event.getWhen());
      }

    });

    return possibleTimes;
  }

  /** Checks if there is overlap between attendees for the request and the given event */
  private static OverlapType isAttendeeOverlap(Collection<String> requiredAttendees,
      Collection<String> optionalAttendees, Event event) {
    // First check if there are people in this meeting that must be at the requested one, then check
    // for optional attendees
    if (event.getAttendees().stream().anyMatch(e -> requiredAttendees.contains(e))) {
      return OverlapType.REQUIRED;
    } else if (event.getAttendees().stream().anyMatch(e -> optionalAttendees.contains(e))) {
      return OverlapType.OPTIONAL;
    }

    // No overlap at all!
    return OverlapType.NONE;
  }

  /** Remove the time taken up by {@code toSubtract} from available times */
  private static void subtractTime(Set<TimeRange> times, TimeRange toSubtract) {
    // Remove times that are within the range (inclusive) of toSubtract
    times.removeIf(time -> toSubtract.contains(time));

    // Eke out any times that have the offending range within them
    List<TimeRange> containTime =
        times.stream().filter(time -> time.contains(toSubtract)).collect(Collectors.toList());
    // Break up times that are larger than toSubtract
    

  }
}
