package net.aquadc.lychee.http.server


// The attempt to wrap Ktor is failed.
// * call.parameters store both query parameters and path parameters https://github.com/ktorio/ktor/issues/1015;
// * there's no difference between query parameter with no value (`?q`) and empty value (`?q=`),
//   there's a similar issue with Locations https://github.com/ktorio/ktor/issues/24, but Parameters work the same way.
