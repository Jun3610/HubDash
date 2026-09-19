class Product:

    def __init__(self, name, price, stock):
      self.name = name
      self.price = price 
      self.stock = stock 
      
    def is_low_stock(self):
        if (self.stock < 5):
          return True
        return False 
      
    def inventory_value(self):
        # 여기에 코드를 작성하세요.
        pass


product1 = Product("Keyboard", 30000, 3)
product2 = Product("Mouse", 15000, 12)

product1.is_low_stock()   # True
product2.is_low_stock()   # False