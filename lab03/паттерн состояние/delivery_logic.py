from abc import ABC, abstractmethod
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

class OrderState(ABC):
    @abstractmethod
    def name(self) -> str:
        pass

    def next(self, order: 'Order'):
        raise ValueError(f"Невозможно перейти из состояния '{self.name()}'")

    def cancel(self, order: 'Order'):
        raise ValueError(f"Невозможно отменить из состояния '{self.name()}'")

class OrderPlacedState(OrderState):
    def name(self) -> str:
        return "Заказ оформлен"

    def next(self, order: 'Order'):
        order.change_state(AtWarehouseState())

    def cancel(self, order: 'Order'):
        order.change_state(CancelledState())

class AtWarehouseState(OrderState):
    def name(self) -> str:
        return "На складе"

    def next(self, order: 'Order'):
        order.change_state(WithCourierState())

    def cancel(self, order: 'Order'):
        order.change_state(CancelledState())

class WithCourierState(OrderState):
    def name(self) -> str:
        return "Передан курьеру"

    def next(self, order: 'Order'):
        order.change_state(InTransitState())

    def cancel(self, order: 'Order'):
        raise ValueError("Отмена запрещена: заказ уже передан курьеру")

class InTransitState(OrderState):
    def name(self) -> str:
        return "В пути"

    def next(self, order: 'Order'):
        moved = order.route_map.move_to_next_point()
        if moved:
            point = order.route_map.get_current_point()
            order.notify_customer(f"Товар прибыл в {point['name']}")
            if "пункт выдачи" in point['name'].lower():
                order.change_state(AtPickupPointState())
            elif order.route_map.current_index == len(order.route_map.points) - 1:
                order.change_state(DeliveredState())
            else:
                order._after_state_change(keep_state=True)
        else:
            order.change_state(DeliveredState())

    def cancel(self, order: 'Order'):
        raise ValueError("Отмена запрещена: товар уже в пути")

class AtPickupPointState(OrderState):
    def name(self) -> str:
        return "В пункте выдачи"

    def next(self, order: 'Order'):
        order.change_state(DeliveredState())

    def cancel(self, order: 'Order'):
        order.change_state(CancelledState())

class DeliveredState(OrderState):
    def name(self) -> str:
        return "Доставлен"

    def next(self, order: 'Order'):
        raise ValueError("Заказ уже доставлен")

    def cancel(self, order: 'Order'):
        raise ValueError("Нельзя отменить доставленный заказ")

class CancelledState(OrderState):
    def name(self) -> str:
        return "Отменён"

    def next(self, order: 'Order'):
        raise ValueError("Отменённый заказ нельзя изменить")

    def cancel(self, order: 'Order'):
        raise ValueError("Заказ уже отменён")

class Order:
    def __init__(self, order_id: str, customer: str, warehouse: str, courier: str, delivery_address: str):
        self.order_id = order_id
        self.customer = customer
        self.warehouse = warehouse
        self.courier = courier
        self.delivery_address = delivery_address
        self.route_map = RouteMap()
        self.state = OrderPlacedState()
        self._route_locked = False
        self._after_state_change()

    def set_route(self, points: List[Dict[str, Any]]):
        if self._route_locked:
            raise ValueError("Маршрут уже сохранён и не может быть изменён")
        self.route_map.set_points(points)
        self._route_locked = True
        self._after_state_change()

    def change_state(self, new_state: OrderState):
        self.state = new_state
        self._after_state_change()

    def _after_state_change(self, keep_state=False):
        self.notify_customer(f"Статус: {self.state.name()}")
        if isinstance(self.state, AtWarehouseState):
            Messenger.send_warehouse(self.warehouse, f"Заказ {self.order_id} на складе")
        elif isinstance(self.state, WithCourierState):
            Messenger.send_courier(self.courier, f"Заказ {self.order_id} передан курьеру")
        elif isinstance(self.state, AtPickupPointState):
            self.notify_customer(f"Заказ ожидает в пункте выдачи")
        elif isinstance(self.state, DeliveredState):
            self.notify_customer(f"Заказ доставлен! Спасибо за покупку.")
        elif isinstance(self.state, CancelledState):
            self.notify_customer(f"Заказ отменён")

    def notify_customer(self, msg):
        Messenger.send_customer(self.customer, msg)

    def next(self):
        self.state.next(self)

    def cancel(self):
        self.state.cancel(self)

    def get_status(self):
        return self.state.name()

    def is_route_locked(self):
        return self._route_locked

    def to_dict(self):
        return {
            "order_id": self.order_id,
            "customer": self.customer,
            "status": self.get_status(),
            "route": self.route_map.to_dict(),
            "delivery_address": self.delivery_address,
            "route_locked": self._route_locked
        }