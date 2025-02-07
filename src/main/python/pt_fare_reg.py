
import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt

from scipy import stats
from sklearn.linear_model import LinearRegression


#%%

df = pd.read_csv("fare.csv")

df = df[df.price.notna()]

# 50 will be long distance
df = df[df.dist < 50000]

#%%


plt = sns.regplot(x="dist", y="price", data=df);


#%%

# Fit slope to intercept 2€, which is the minimum

model = LinearRegression(fit_intercept=True)

model.fit(df.dist.to_numpy().reshape(-1, 1), df.price)

print(model.coef_, model.intercept_)

#%%
