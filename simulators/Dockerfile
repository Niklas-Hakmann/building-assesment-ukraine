FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY simulator.py sensors.json ./
CMD ["python", "-u", "simulator.py"]
