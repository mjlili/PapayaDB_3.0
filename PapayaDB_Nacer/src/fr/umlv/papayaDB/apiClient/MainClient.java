package fr.umlv.papayaDB.apiClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Scanner;

import fr.umlv.papayaDB.apiClient.ApiClient.DBQuery;

public class MainClient {

	public static ClassValue<Method[]> cv = new ClassValue<Method[]>() {
		@Override
		protected Method[] computeValue(Class<?> type) {
			return type.getMethods();
		}
	};

	private static String call(Method method, Object receiver, Object[] arg) {
		try {
			if (arg.length > 1) {
				return (String) method.invoke(receiver, arg[0], arg[1]);
			}
			return (String) method.invoke(receiver, arg[0]);

		} catch (IllegalAccessException e) {
			throw new IllegalStateException("should not happen");
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
			throw new UndeclaredThrowableException(t);
		}
	}

	public static void main(String[] args) {

		try (Scanner sc = new Scanner(System.in)) {
			ApiClient httpClient;
			try {
				httpClient = new ApiClient();
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"The REST server address " + ApiClient.SERVER_ADDRESS + "is not valid.");
			}

			while (true) {
				System.out.println("Enter your query :");
				String userQuery = sc.nextLine();
				if (userQuery.equals("exit")) {
					break;
				}
				try {
					String[] commande = userQuery.split("\\s->\\s", 2);
					String[] argument = commande[1].split("\\s");
					Arrays.stream(cv.get(httpClient.getClass())).filter(m -> m.isAnnotationPresent(DBQuery.class))
							.filter(m -> m.getAnnotation(DBQuery.class).value().equalsIgnoreCase(commande[0]))
							.forEach(m -> System.out.println(call(m, httpClient, argument)));
				} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
					System.out.println("request wrong unrecognize, please respect the syntax.");
				}
				System.out.println(httpClient.getAllDatabases());
			}
		}
	}
}