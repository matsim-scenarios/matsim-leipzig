import argparse
import gzip
import random
import math
from bisect import bisect
from collections import defaultdict
from time import sleep

import pandas as pd
import bs4

from tqdm import tqdm
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.common.exceptions import ElementClickInterceptedException


def extract_fare(driver, c):
    """ Extract fare price """

    driver.get("https://reiseauskunft.bahn.de")
    sleep(1)

    # Cookie box
    div = driver.find_element(By.TAG_NAME, "div")
    # Cookie popup
    if div.get_attribute("id") != "doc":
        div.shadow_root.find_element(By.CSS_SELECTOR, "button.js-accept-all-cookies").click()

    f = driver.find_element(By.CSS_SELECTOR, "input.from")
    f.send_keys(c["from"])
    f.click()

    t = driver.find_element(By.CSS_SELECTOR, "input.to")
    t.send_keys(c["to"])
    t.click()

    date = driver.find_element(By.ID, "REQ0JourneyDate")
    date.clear()
    # TODO: configurable
    date.send_keys("Do, 28.04.2022")

    time = driver.find_element(By.ID, "REQ0JourneyTime")
    time.clear()
    time.send_keys("11:00")

    driver.find_element(By.ID, "searchConnectionButton").click()

    # confirm corrected stations
    try:
        WebDriverWait(driver, 3).until(EC.element_to_be_clickable((By.ID, "searchConnectionButton"))).click()
    except:
        pass

    WebDriverWait(driver, 5).until(EC.element_to_be_clickable((By.CSS_SELECTOR, "a.buttonbold"))).click()

    el = driver.find_element(By.XPATH, "//td[contains(text(), 'MDV Einzelfahrkarte')]/following-sibling::td")
    price = el.text.split("EUR")
    c["price"] = float(price[0].replace(",", "."))


if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Extract PT fare prices")
    parser.add_argument("--input", default="../../../scenarios/input/leipzig-v1.1-transitSchedule.xml.gz",
                        help="Path to transit schedule", required=False)
    parser.add_argument("--output", default="fare.csv")

    args = parser.parse_args()

    with gzip.open(args.input, 'rb') as f:
        content = f.read()
        doc = bs4.BeautifulSoup(content, "xml")

    stops = {}

    for stop in doc.find_all('stopFacility'):
        name = stop.attrs["name"]
        stops[name] = {
            "name": stop.attrs["name"],
            "x": float(stop.attrs["x"]),
            "y": float(stop.attrs["y"])
        }

    stops = list(stops.values())

    print("Read %d stops" % len(stops))

    bins = [0, 2, 4, 6, 8, 10, 15, 20, 30, 40, 50]

    conn = defaultdict(lambda: [])

    for j in range(10000):
        f = random.choice(stops)
        t = random.choice(stops)

        if f == t:
            continue

        dist = math.sqrt((f["x"] - t["x"]) ** 2 + (f["y"] - t["y"]) ** 2)

        idx = bisect(bins, dist / 1000)

        if len(conn[idx]) > 10:
            continue

        conn[idx].append({
            "from": f["name"],
            "to": t["name"],
            "dist": dist
        })

    flatten = [item for sublist in conn.values() for item in sublist]

    driver = webdriver.Chrome()

    for c in tqdm(flatten):

        try:
            extract_fare(driver, c)
        except Exception as e:
            print(e)

        sleep(5)

    df = pd.DataFrame(flatten)
    df.to_csv(args.output, index=False)
