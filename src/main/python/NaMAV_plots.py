import plotly.express as px
import pandas as pd
import os
import plotly.graph_objects as go


def generate_pie_chart(title, csv_file_path):

    # Read input and rename modes
    df = pd.read_csv(csv_file_path, sep=",", header=None)
    df = df.drop(0)
    df.columns = df.iloc[0]
    df = df.drop(1).reset_index(drop=True)
    mode_mapping = {
        'bike': 'Bicycle',
        'car': 'Car',
        'pt': 'Public transport',
        'ride': 'Car as passenger',
        'walk': 'Walking'
    }
    df = df.rename(columns=mode_mapping)

    unwanted_modes = ['drtNorth', 'drtSoutheast']

    # Removing drNorth and drtSouth if they exist
    for mode in unwanted_modes:
        if mode in df.columns:
            df.drop(columns=mode, inplace=True)

    # data preparation (transport + removing count column)
    data_for_plot = df.transpose()
    data_for_plot.columns = ['Count', 'Percentage']
    data_for_plot = data_for_plot.drop(columns='Count')

    # chart setting
    colors = ['#fc8d62', '#66c2a5', '#8da0cb', '#a6d854', '#e78ac3', '#ffd92f']
    fig = px.pie(data_for_plot, names=data_for_plot.index, values='Percentage', title=title,
                 color_discrete_sequence=colors)
    font_family = "Times New Roman, serif"

    fig.update_traces(textinfo='percent+label', marker=dict(line=dict(color='#000000', width=1)))
    fig.update_layout(
        font=dict(family=font_family, size=11, color="black"),
        title_font=dict(size=20, family=font_family, color="black"),
        legend_font=dict(size=11, family=font_family, color="black")
    )
    # fig.show()

    base_filename = os.path.basename(csv_file_path).rsplit('.', 1)[0]
    output_filename = f"pdf_plots/plotly.plot.{base_filename}.pdf"
    fig.write_image(output_filename)
    print(f"Saved plot as {output_filename}")


def generate_bar_chart(title, csv_file_path, x_axis_title, y_axis_title):

    # Read input
    df = pd.read_csv(csv_file_path, sep=",")

    df = df.rename(columns={"policy": "large car-free area"})

    x_value = df.columns[0]
    y_values = df.columns[1:].tolist()

    fig = px.bar(df, x=x_value, y=y_values, title=title, barmode='group')

    font_family = "Times New Roman, serif"

    fig.update_layout(
        font=dict(family=font_family, size=14, color="black"),
        title_font=dict(size=20, family=font_family, color="black"),
        legend_font=dict(size=14, family=font_family, color="black"),
        legend_title_text='Scenario:',
        xaxis_title=x_axis_title,
        yaxis_title=y_axis_title,
        legend_orientation="h",
        legend=dict(y=-0.2, traceorder='normal')
    )

    #fig.show()

    # Generate pdf
    base_filename = os.path.basename(csv_file_path).rsplit('.', 1)[0]
    output_filename = f"pdf_plots/plotly.plot_{base_filename}.pdf"
    fig.write_image(output_filename)
    print(f"Saved plot as {output_filename}")


def generate_sankey_chart(title, csv_file_path):

    # Read input
    df = pd.read_csv(csv_file_path, sep=",")

    modes = list(pd.concat([df['main_mode.x'], df['main_mode.y']]).unique())
    mode_to_index = {mode: i for i, mode in enumerate(modes)}

    source = df['main_mode.x'].map(mode_to_index).values
    target = df['main_mode.y'].map(mode_to_index).values
    value = df['Freq'].values

    labels = modes * 2
    adjusted_source = source
    adjusted_target = [idx + len(modes) for idx in target]

    fig = go.Figure(go.Sankey(
        node=dict(
            pad=15,
            thickness=20,
            line=dict(color="black", width=0.5),
            label=labels,
        ),
        link=dict(
            source=adjusted_source,
            target=adjusted_target,
            value=value
        )
    ))
    fig.update_layout(title_text="Modal shift of region area",  font=dict(size=8, color='black', family='Times New Roman'))

    fig.show()

    # Generate pdf
    base_filename = os.path.basename(csv_file_path).rsplit('.', 1)[0]
    output_filename = f"pdf_plots/plotly.plot_{base_filename}.pdf"
    fig.write_image(output_filename)
    print(f"Saved plot as {output_filename}")

def generate_line_chart(title, csv_file_path, x_axis_title, y_axis_title):

    # Read input
    df = pd.read_csv(csv_file_path, sep=",")

    df = df.rename(columns={"policy": "large car-free area"})

    x_value = df.columns[0]
    y_values = df.columns[1:].tolist()

    fig = px.line(df, x=x_value, y=y_values, title=title)

    font_family = "Times New Roman, serif"

    fig.update_layout(
        font=dict(family=font_family, size=14, color="black"),
        title_font=dict(size=20, family=font_family, color="black"),
        legend_font=dict(size=14, family=font_family, color="black"),
        legend_title_text='Scenario:',
        xaxis_title=x_axis_title,
        yaxis_title=y_axis_title,
        legend_orientation="h",
        legend=dict(y=-0.2, traceorder='normal')
    )

    #fig.show()

    # Generate pdf
    base_filename = os.path.basename(csv_file_path).rsplit('.', 1)[0]
    output_filename = f"pdf_plots/plotly.plot_line{base_filename}.pdf"
    fig.write_image(output_filename)
    print(f"Saved plot as {output_filename}")


# ========================
# Calling Functions of Plots
# ========================

# ------------------------
# Bar Charts
# ------------------------

# Number of trips

generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.region.TUD.csv','Main trip mode','Number of trips')
generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.city.TUD.csv','Main trip mode','Number of trips')
generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.carfree.area.TUD.csv','Main trip mode','Number of trips')
generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.TFW.carfree.area.TUD.csv','Main trip mode','Number of trips')
generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.residents.carfree.area.TUD.csv','Main trip mode','Number of trips')
generate_bar_chart("Modal split by trip counts", 'results_sources/df.trips.number.by.mode.workers.carfree.area.TUD.csv','Main trip mode','Number of trips')

# Average distance of trips shifted from car to other trips

generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.region.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.city.TUD.csv','Main trip mode', 'Average distance (m)')
generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.carfree.area.TUD.csv','Main trip mode', 'Average distance (m)')
generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.TFW.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.residents.carfree.area.TUD.csv','Main trip mode', 'Average distance (m)')
generate_bar_chart("Average distance of trips shifted from car to other modes", 'results_sources/df.car.shifted.trips.average.distance.by.mode.workers.carfree.area.TUD.csv','Main trip mode', 'Average distance (m)')

#  Total distance (based on main mode of trip)

generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.region.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.city.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.carfree.area.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.TFW.carfree.area.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.residents.carfree.area.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total distance (based on main mode of trip)", 'results_sources/df.total.distance.by.mode.workers.carfree.area.TUD.csv','Main trip mode', 'Total distance (km)')

# Average travel distance (based on main mode of trip)

generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.region.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.city.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.TFW.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.residents.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (based on main mode of trip)", 'results_sources/df.average.distance.by.mode.trip.based.workers.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')

# Average travel distance per person

generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.region.TUD.csv','Main trip mode', 'Average distance per person (km)')
generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.city.TUD.csv','Main trip mode', 'Average distance per person (km)')
generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.carfree.area.TUD.csv','Main trip mode', 'Average distance per person (km)')
generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.TFW.carfree.area.TUD.csv','Main trip mode', 'Average distance per person (km)')
generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.residents.carfree.area.TUD.csv','Main trip mode', 'Average distance per person (km)')
generate_bar_chart("Average travel distance per person", 'results_sources/df.average.distance.by.mode.person.based.workers.carfree.area.TUD.csv','Main trip mode', 'Average distance per person (km)')

# Total distance (leg based)

generate_bar_chart("Total travel distance (leg based)", 'results_sources/df.total.distance.by.mode.leg.based.region.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total travel distance (leg based)", 'results_sources/df.total.distance.by.mode.leg.based.city.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Total travel distance (leg based)", 'results_sources/df.total.distance.by.mode.leg.based.carfree.area.TUD.csv','Main trip mode', 'Total distance (km)')

# Average travel distance (leg based)

generate_bar_chart("Average travel distance (leg based)", 'results_sources/df.average.distance.by.mode.leg.based.region.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (leg based)", 'results_sources/df.average.distance.by.mode.leg.based.city.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (leg based)", 'results_sources/df.average.distance.by.mode.leg.based.carfree.area.TUD.csv','Main trip mode', 'Average distance (km)')

# Total distance (main leg of the trip)

generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.region.csv.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.city.csv.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.carfree.area.csv.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.TFW.carfree.area.csv.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.residents.carfree.area.csv.TUD.csv','Main trip mode', 'Total distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.total.distance.by.mode.main.leg.workers.carfree.area.csv.TUD.csv','Main trip mode', 'Total distance (km)')

# Average travel distance (main leg of the trip)

generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.region.csv.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.city.csv.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.carfree.area.csv.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.TFW.carfree.area.csv.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.residents.carfree.area.csv.TUD.csv','Main trip mode', 'Average distance (km)')
generate_bar_chart("Average travel distance (main leg of the trip)", 'results_sources/df.average.distance.by.mode.main.leg.workers.carfree.area.csv.TUD.csv','Main trip mode', 'Average distance (km)')

# Average walking distance by main mode

generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.region.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')
generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.city.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')
generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.carfree.area.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')
generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.TFW.carfree.area.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')
generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.residents.carfree.area.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')
generate_bar_chart("Average walking distance by main mode", 'results_sources/df.average.walking.distance.by.mode.workers.carfree.area.TUD.csv', 'Main trip mode', 'Average  walking distance (m)')

# Number of trips by walking distance interval

generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.region.car.TUD.csv','Distance interval', 'Trips counts')
generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.city.car.TUD.csv','Distance interval', 'Trips counts')
generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.TFW.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.residents.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_bar_chart("Number of car trips by walking distance interval",'results_sources/df.walking.distance.distribution.by.mode.workers.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')

# Average travel time (trip based)

generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.trip.based.region.TUD.csv','Main trip mode', 'Average travel time (min)')
generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.trip.based.city.TUD.csv','Main trip mode', 'Average travel time (min)')
generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.carfree.trip.based.area.TUD.csv','Main trip mode', 'Average travel time (min)')
generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.TFW.carfree.trip.based.area.TUD.csv','Main trip mode', 'Average travel time (min)')
generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.residents.carfree.trip.based.area.TUD.csv','Main trip mode', 'Average travel time (min)')
generate_bar_chart("Average travel time (trip based)", 'results_sources/df.travel.time.by.mode.workers.carfree.trip.based.area.TUD.csv','Main trip mode', 'Average travel time (min)')

# Average travel time (leg based)

generate_bar_chart("Average travel time (leg based)", 'results_sources/df.travel.time.by.mode.leg.based.region.TUD.csv', 'Main trip mode', 'Average travel time(min)')
generate_bar_chart("Average travel time (leg based)", 'results_sources/df.travel.time.by.mode.leg.based.city.TUD.csv', 'Main trip mode', 'Average travel time(min)')
generate_bar_chart("Average travel time (leg based)", 'results_sources/df.travel.time.by.mode.leg.based.carfree.area.TUD.csv', 'Main trip mode', 'Average travel time(min)')

# Average speed (trip based)

generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.region.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.city.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.carfree.area.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.TFW.carfree.area.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.residents.carfree.area.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (trip based)", 'results_sources/df.average.speed.by.mode.trip.based.workers.carfree.area.TUD.csv', 'Main trip mode', 'Average speed (m/s)')

# Average speed (leg based)

generate_bar_chart("Average speed (leg based)", 'results_sources/df.average.speed.by.mode.leg.based.region.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (leg based)", 'results_sources/df.average.speed.by.mode.leg.based.city.TUD.csv', 'Main trip mode', 'Average speed (m/s)')
generate_bar_chart("Average speed (leg based)", 'results_sources/df.average.speed.by.mode.leg.based.carfree.area.TUD.csv', 'Main trip mode', 'Average speed (m/s)')

# ------------------------
# Line Charts
# ------------------------

generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.region.car.TUD.csv','Distance interval', 'Trips counts')
generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.city.car.TUD.csv','Distance interval', 'Trips counts')
generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.TFW.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.residents.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')
generate_line_chart("Number of car trips by walking distance distribution",'results_sources/df.walking.distance.distribution.by.mode.workers.carfree.area.car.TUD.csv','Distance interval', 'Trips counts')


# ------------------------
# Pie charts
# ------------------------

# Modal split by counts (Trips)

# base
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.region.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.city.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.TFW.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.residetns.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.base.workers.carfree.area.TUD.csv')

# policy
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.region.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.city.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.TFW.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.residetns.carfree.area.TUD.csv')
generate_pie_chart("Modal split by trip counts", 'results_sources/df.pie.ms.counts.trips.policy.workers.carfree.area.TUD.csv')


# Modal split by distance (Trips)

# base
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.region.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.city.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.TFW.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.residetns.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.base.workers.carfree.area.TUD.csv')

# policy
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.region.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.city.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.TFW.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.residetns.carfree.area.TUD.csv')
generate_pie_chart("Modal split by distance", 'results_sources/df.pie.ms.distance.trips.policy.workers.carfree.area.TUD.csv')

# Modal split by counts (legs)

# base
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.base.region.TUD.csv')
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.base.city.TUD.csv')
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.base.carfree.area.TUD.csv')

#legs
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.policy.region.TUD.csv')
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.policy.city.TUD.csv')
generate_pie_chart("Modal split by trip counts",'results_sources/df.pie.ms.counts.legs.policy.carfree.area.TUD.csv')


# ------------------------
# Sankey charts
# ------------------------

generate_sankey_chart("Modal shift of region area", 'results_sources/df.sankey.region.TUD.csv')
generate_sankey_chart("Modal shift of city area", 'results_sources/df.sankey.city.TUD.csv')
generate_sankey_chart("Modal shift of car-free area", 'results_sources/df.sankey.carfree.area.TUD.csv')
generate_sankey_chart("Modal shift of to, from and within car-free area", 'results_sources/df.sankey.TFW.carfree.area.TUD.csv')
generate_sankey_chart("Modal shift of residents of car-free area", 'results_sources/df.sankey.residents.carfree.area.TUD.csv')
generate_sankey_chart("Modal shift of workers of car free-area", 'results_sources/df.sankey.workers.carfree.area.TUD.csv')