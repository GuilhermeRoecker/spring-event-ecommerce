export type Role = "CUSTOMER" | "SELLER" | "OWNER" | "ADMIN";
export type OrderStatus = "AWAITING_PAYMENT" | "PAID" | "SHIPPED" | "DELIVERED";
export type PaymentMethod = "PIX" | "CARD" | "BOLETO";
export interface Profile {
  id: number;
  name: string;
  email: string;
  role: Role;
  companyId: number | null;
}
export interface Member {
  id: number;
  name: string;
  email: string;
  role: Role;
  company_id: number | null;
}
export interface Company {
  id: number;
  name: string;
  legal_name: string;
}
export interface Category {
  id: number;
  name: string;
}
export interface Product {
  id: number;
  company_id: number;
  category_id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  image_url: string | null;
}
export interface ManagedProduct extends Product {
  active: boolean;
}
export interface ProductInput {
  companyId: number;
  categoryId: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  active: boolean;
  imageUrl: string | null;
}
export interface AddressInput {
  street: string;
  number: string;
  complement: string | null;
  city: string;
  state: string;
  postalCode: string;
}
export interface Address {
  id: number;
  street: string;
  number: string;
  complement: string | null;
  city: string;
  state: string;
  postal_code: string;
}
export interface CartItem {
  product_id: number;
  name: string;
  price: number;
  stock: number;
  active: boolean;
  quantity: number;
}
export interface CheckoutInput {
  addressId: number;
  method: PaymentMethod;
  coupon: string | null;
}
export interface CheckoutResult {
  id: number;
  total: number;
  status: OrderStatus;
}
export interface Order {
  id: number;
  created_at: string;
  status: OrderStatus;
  subtotal: number;
  discount: number;
  total: number;
}
export interface OrderItem {
  product_id: number;
  name: string;
  quantity: number;
  unit_price: number;
}
export interface OrderDetail {
  order: Order & { shipping_address: string };
  items: OrderItem[];
  payment: { method: PaymentMethod; status: string };
  delivery: {
    status: string;
    carrier: string | null;
    tracking_code: string | null;
  };
}
export interface Coupon {
  id: number;
  code: string;
  percentage: number;
  expires_on: string;
  usage_limit: number;
  used: number;
  active: boolean;
}
export interface CouponInput {
  code: string;
  percentage: number;
  expiresOn: string;
  usageLimit: number;
}
export interface Sale {
  order_id: number;
  product_id: number;
  name: string;
  quantity: number;
  unit_price: number;
  created_at: string;
  status: OrderStatus;
}
export interface CsrfToken {
  token: string;
  headerName: string;
}
export const ROLE_NAMES: Record<Role, string> = {
  CUSTOMER: "Cliente",
  SELLER: "Vendedor",
  OWNER: "Dono de loja",
  ADMIN: "Administrador",
};
export const STATUS_NAMES: Record<string, string> = {
  AWAITING_PAYMENT: "Aguardando pagamento",
  PAID: "Pago",
  SHIPPED: "Enviado",
  DELIVERED: "Entregue",
  PENDING: "Pendente",
};
export const STATES =
  "AC AL AP AM BA CE DF ES GO MA MT MS MG PA PB PR PE PI RJ RN RS RO RR SC SP SE TO".split(
    " ",
  );
