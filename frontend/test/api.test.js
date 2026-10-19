import { test, afterEach } from "node:test";
import assert from "node:assert/strict";
import { fetchSection, getMusic, isValidYear } from "../src/js/api.js";

const realFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = realFetch;
});

const okResponse = (body) => ({ ok: true, status: 200, json: async () => body });

test("fetchSection requests the plural years route on port 8080", async () => {
  let requested;
  globalThis.fetch = async (url) => {
    requested = String(url);
    return okResponse({});
  };
  await fetchSection(1994, "events");
  assert.equal(requested, "http://127.0.0.1:8080/api/v1/years/1994/events");
});

test("fetchSection throws on a non-OK status", async () => {
  globalThis.fetch = async () => ({ ok: false, status: 502 });
  await assert.rejects(fetchSection(1994, "events"), { message: "API error: 502" });
});

test("getMusic shares one request between concurrent calls for a year", async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return okResponse({ year: 2001 });
  };
  const [a, b] = await Promise.all([getMusic(2001), getMusic(2001)]);
  assert.equal(calls, 1);
  assert.deepEqual(a, { year: 2001 });
  assert.equal(a, b);
});

test("getMusic retries after a failed request", async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    if (calls === 1) throw new Error("network down");
    return okResponse({ year: 2002 });
  };
  await assert.rejects(getMusic(2002), { message: "network down" });
  assert.deepEqual(await getMusic(2002), { year: 2002 });
  assert.equal(calls, 2);
});

test("getMusic requests a year again once the earlier request has settled", async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return okResponse({ year: 2003 });
  };
  await getMusic(2003);
  await getMusic(2003);
  assert.equal(calls, 2);
});

test("getMusic shares one request between a string and a numeric year", async () => {
  let calls = 0;
  globalThis.fetch = async () => {
    calls++;
    return okResponse({ year: 2004 });
  };
  const [a, b] = await Promise.all([getMusic("2004"), getMusic(2004)]);
  assert.equal(calls, 1);
  assert.equal(a, b);
});

test("isValidYear accepts whole years from 1 to this year only", () => {
  const thisYear = new Date().getFullYear();
  for (const ok of ["1", "1994", String(thisYear)]) assert.equal(isValidYear(ok), true, ok);
  for (const bad of ["", "0", "abc", "19.5", "-5", "1e3", String(thisYear + 1)]) assert.equal(isValidYear(bad), false, bad);
});
