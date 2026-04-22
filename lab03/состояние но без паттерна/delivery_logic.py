from typing import List, Dict, Any

class RouteMap:
    def __init__(self, points: List[Dict[str, Any]] = None):
        self.points = points if points else []
        self.current_index = 0

    def set_points(self, points: List[Dict[str, Any]]):
        self.points = points
        self.current_index = 0

    def get_current_point(self) -> Dict[str, Any]:
        if self.points and self.current_index < len(self.points):
            return self.points[self.current_index]
        return None

    def move_to_next_point(self) -> bool:
        if self.current_index < len(self.points) - 1:
            self.current_index += 1
            return True
        return False

    def to_dict(self):
        return {
            "points": self.points,
            "current_index": self.current_index
        }

class Messenger:
    customer_notifications = []
    warehouse_notifications = []
    courier_notifications = []

    @classmethod
    def clear(cls):
        cls.customer_notifications = []
        cls.warehouse_notifications = []
        cls.courier_notifications = []

    @classmethod
    def send_customer(cls, customer_name: str, message: str):
        cls.customer_notifications.append(f"[{customer_name}] {message}")

    @classmethod
    def send_warehouse(cls, warehouse_name: str, message: str):
        cls.warehouse_notifications.append(f"[{warehouse_name}] {message}")

    @classmethod
    def send_courier(cls, courier_name: str, message: str):
        cls.courier_notifications.append(f"[{courier_name}] {message}")

    @classmethod
    def get_all(cls):
        return {
            "customer": cls.customer_notifications,
            "warehouse": cls.warehouse_notifications,
            "courier": cls.courier_notifications
        }

class Order:
    def __init__(self, order_id: str, customer: str, warehouse: str, courier: str, delivery_address: str):
        self.order_id = order_id
        self.customer = customer
        self.warehouse = warehouse
        self.courier = courier
        self.delivery_address = delivery_address
        self.route_map = RouteMap()
        self.status = "Заказ оформлен"
        self._route_locked = False
        self._notify_status_change()

    def _notify_status_change(self):
        Messenger.send_customer(self.customer, f"Статус: {self.status}")
        if self.status == "На складе":
            Messenger.send_warehouse(self.warehouse, f"Заказ {self.order_id} на складе")
        elif self.status == "Передан курьеру":
            Messenger.send_courier(self.courier, f"Заказ {self.order_id} передан курьеру")
        elif self.status == "В пункте выдачи":
            Messenger.send_customer(self.customer, f"Заказ ожидает в пункте выдачи")
        elif self.status == "Доставлен":
            Messenger.send_customer(self.customer, f"Заказ доставлен! Спасибо за покупку.")
        elif self.status == "Отменён":
            Messenger.send_customer(self.customer, f"Заказ отменён")

    def set_route(self, points: List[Dict[str, Any]]):
        if self._route_locked:
            raise ValueError("Маршрут уже сохранён и не может быть изменён")
        self.route_map.set_points(points)
        self._route_locked = True
        self._notify_status_change()

    def next(self):
        if self.status == "Заказ оформлен":
            self.status = "На складе"
            self._notify_status_change()
        elif self.status == "На складе":
            self.status = "Передан курьеру"
            self._notify_status_change()
        elif self.status == "Передан курьеру":
            self.status = "В пути"
            self._notify_status_change()
        elif self.status == "В пути":
            moved = self.route_map.move_to_next_point()
            if moved:
                point = self.route_map.get_current_point()
                Messenger.send_customer(self.customer, f"Товар прибыл в {point['name']}")
                if "пункт выдачи" in point['name'].lower():
                    self.status = "В пункте выдачи"
                    self._notify_status_change()
                elif self.route_map.current_index == len(self.route_map.points) - 1:
                    self.status = "Доставлен"
                    self._notify_status_change()
                else:
                    pass
            else:
                self.status = "Доставлен"
                self._notify_status_change()
        elif self.status == "В пункте выдачи":
            self.status = "Доставлен"
            self._notify_status_change()
        elif self.status == "Доставлен":
            raise ValueError("Заказ уже доставлен")
        elif self.status == "Отменён":
            raise ValueError("Отменённый заказ нельзя изменить")
        else:
            raise ValueError(f"Невозможно выполнить переход из статуса {self.status}")

    def cancel(self):
        if self.status in ["Заказ оформлен", "На складе", "В пункте выдачи"]:
            self.status = "Отменён"
            self._notify_status_change()
        elif self.status == "Отменён":
            raise ValueError("Заказ уже отменён")
        else:
            raise ValueError(f"Невозможно отменить заказ в статусе {self.status}")

    def get_status(self):
        return self.status

    def is_route_locked(self):
        return self._route_locked

    def to_dict(self):
        return {
            "order_id": self.order_id,
            "customer": self.customer,
            "status": self.status,
            "route": self.route_map.to_dict(),
            "delivery_address": self.delivery_address,
            "route_locked": self._route_locked
        }