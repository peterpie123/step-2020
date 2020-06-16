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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Contains various utility methods related to TimeRange manipulation */
public class QueryUtil {
  /** Have end time for meetings be exclusive. */
  public static final boolean TIME_EXCLUSIVE = false;
  /** Have end time for meetings be inclusive. */
  public static final boolean TIME_INCLUSIVE = true;

  /** Remove the time taken up by {@code toSubtract} from available times. */
  public static void subtractTime(Collection<TimeRange> availableTimes, TimeRange toSubtract) {
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

  /** Returns any overlap between the given list of times and the time in question. */
  public static List<TimeRange> overlap(Collection<TimeRange> times, TimeRange overlap) {
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
  public static List<TimeRange> overlap(Collection<TimeRange> times1,
      Collection<TimeRange> times2) {
    List<TimeRange> out = new ArrayList<>();

    times1.forEach(time -> {
      out.addAll(overlap(times2, time));
    });

    return out;
  }

  /** Combine the available times of the given attendees */
  public static List<TimeRange> combineTimes(Map<String, List<TimeRange>> times,
      Set<String> attendees) {
    List<TimeRange> out = new ArrayList<>();
    out.add(TimeRange.WHOLE_DAY);

    attendees.forEach(attendee -> {
      List<TimeRange> overlap = overlap(out, times.get(attendee));
      // Workaround as can't do out = ... in a lambda
      out.clear();
      out.addAll(overlap);
    });
    return out;
  }
}
